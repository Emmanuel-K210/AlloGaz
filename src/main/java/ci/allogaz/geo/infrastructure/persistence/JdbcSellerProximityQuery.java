package ci.allogaz.geo.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import ci.allogaz.geo.application.port.out.SellerProximityQuery;
import ci.allogaz.geo.domain.SellerCandidate;
import ci.allogaz.geo.domain.SellerCandidate.MatchingOffer;
import ci.allogaz.shared.domain.GeoPoint;

/**
 * Requête PostGIS : ST_DWithin (utilise l'index GiST sur location::geography) pour borner la recherche,
 * ST_Distance pour la distance réelle en mètres. Lecture directe des tables du catalogue (modèle de lecture).
 */
@Repository
class JdbcSellerProximityQuery implements SellerProximityQuery {

    /**
     * Filtre d'offre partagé par les deux requêtes (alias o = offre, p = produit, s = dépôt).
     * La contenance et le type de vente s'appliquent toujours ; la société/marque/couleur de la bouteille
     * (ce que l'acheteur a en main) ne s'applique qu'aux dépôts qui ne font PAS l'échange toutes marques :
     * un point d'échange universel doit remonter même s'il ne vend pas la société recherchée, puisqu'il la
     * reprend quand même en échange d'une autre.
     */
    private static final String OFFER_FILTER = """
            o.active AND p.active
            AND (CAST(:capacity AS integer) IS NULL OR p.capacity_grams = CAST(:capacity AS integer))
            AND (CAST(:saleType AS text) IS NULL
                 OR (CAST(:saleType AS text) = 'REFILL' AND p.refill_price IS NOT NULL)
                 OR (CAST(:saleType AS text) = 'PURCHASE' AND p.purchase_price IS NOT NULL))
            AND (
                  s.universal_exchange
                  OR (
                       (CAST(:productId AS uuid) IS NULL OR p.id = CAST(:productId AS uuid))
                       AND (CAST(:brand AS text) IS NULL OR lower(p.brand) = lower(CAST(:brand AS text)))
                       AND (CAST(:company AS text) IS NULL OR lower(p.company) = lower(CAST(:company AS text)))
                       AND (CAST(:color AS text) IS NULL OR lower(CAST(:color AS text)) = ANY (p.bottle_colors))
                     )
                )
            """;

    private static final String SELLERS_SQL = """
            WITH buyer AS (SELECT ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography AS g)
            SELECT s.id, s.shop_name, s.address, ST_Y(s.location) AS lat, ST_X(s.location) AS lon,
                   ST_Distance(s.location::geography, buyer.g) AS distance_m,
                   s.delivery_radius_m, s.delivery_mode, s.delivery_fee, s.universal_exchange,
                   COALESCE(st.rating_sum, 0) AS rating_sum, COALESCE(st.rating_count, 0) AS rating_count,
                   COALESCE(st.orders_decided, 0) AS orders_decided, COALESCE(st.orders_accepted, 0) AS orders_accepted
            FROM seller_profiles s
            CROSS JOIN buyer
            LEFT JOIN seller_stats st ON st.seller_id = s.id
            WHERE s.status = 'VERIFIED'
              AND s.accepting_orders
              AND ST_DWithin(s.location::geography, buyer.g, :radius)
              AND (NOT EXISTS (SELECT 1 FROM seller_opening_hours h WHERE h.seller_id = s.id)
                   OR EXISTS (SELECT 1 FROM seller_opening_hours h
                              WHERE h.seller_id = s.id AND h.day_of_week = :dow
                                AND CAST(:time AS time) >= h.opens_at AND CAST(:time AS time) < h.closes_at))
              AND EXISTS (SELECT 1 FROM seller_offers o JOIN products p ON p.id = o.product_id
                          WHERE o.seller_id = s.id AND %s)
            ORDER BY distance_m
            LIMIT :limit
            """.formatted(OFFER_FILTER);

    private static final String OFFERS_SQL = """
            SELECT o.id, o.seller_id, p.id AS product_id, p.name, p.brand, p.company, p.bottle_colors, p.appearance,
                   p.capacity_grams, p.refill_price, p.purchase_price, o.stock
            FROM seller_offers o JOIN products p ON p.id = o.product_id JOIN seller_profiles s ON s.id = o.seller_id
            WHERE o.seller_id IN (:sellerIds) AND %s
            ORDER BY o.stock DESC, p.name
            """.formatted(OFFER_FILTER);

    private final JdbcClient jdbc;

    JdbcSellerProximityQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<SellerCandidate> findCandidates(GeoPoint buyer, double maxRadiusMeters, LocalDateTime at,
            Criteria criteria, int limit) {
        Map<String, Object> params = criteriaParams(criteria);
        params.put("lat", buyer.latitude());
        params.put("lon", buyer.longitude());
        params.put("radius", maxRadiusMeters);
        params.put("dow", at.getDayOfWeek().getValue());
        params.put("time", at.toLocalTime());
        params.put("limit", limit);

        List<SellerRow> sellers = jdbc.sql(SELLERS_SQL).params(params).query((rs, i) -> new SellerRow(
                rs.getObject("id", UUID.class), rs.getString("shop_name"), rs.getString("address"),
                rs.getDouble("lat"), rs.getDouble("lon"), rs.getDouble("distance_m"), rs.getInt("delivery_radius_m"),
                rs.getString("delivery_mode"), rs.getLong("delivery_fee"), rs.getBoolean("universal_exchange"),
                rs.getLong("rating_sum"), rs.getLong("rating_count"), rs.getLong("orders_decided"),
                rs.getLong("orders_accepted"))).list();
        if (sellers.isEmpty()) {
            return List.of();
        }

        Map<String, Object> offerParams = criteriaParams(criteria);
        offerParams.put("sellerIds", sellers.stream().map(SellerRow::id).toList());
        Map<UUID, List<MatchingOffer>> offersBySeller = jdbc.sql(OFFERS_SQL).params(offerParams)
                .query((rs, i) -> Map.entry(rs.getObject("seller_id", UUID.class), new MatchingOffer(
                        rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class), rs.getString("name"),
                        rs.getString("brand"), rs.getString("company"),
                        List.of((String[]) rs.getArray("bottle_colors").getArray()), rs.getString("appearance"),
                        (Integer) rs.getObject("capacity_grams"), (Long) rs.getObject("refill_price"),
                        (Long) rs.getObject("purchase_price"), rs.getInt("stock"))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return sellers.stream().map(s -> new SellerCandidate(s.id(), s.shopName(), s.address(), s.lat(), s.lon(),
                s.distance(), s.radius(), s.deliveryMode(), s.deliveryFee(), s.ratingSum(), s.ratingCount(),
                s.decided(), s.accepted(), offersBySeller.getOrDefault(s.id(), List.of()), s.universalExchange()))
                .toList();
    }

    private static Map<String, Object> criteriaParams(Criteria c) {
        Map<String, Object> params = new HashMap<>();
        params.put("productId", c.productId());
        params.put("brand", c.brand());
        params.put("company", c.company());
        params.put("color", c.bottleColor());
        params.put("capacity", c.capacityGrams());
        params.put("saleType", c.saleType() == null ? null : c.saleType().name());
        return params;
    }

    private record SellerRow(UUID id, String shopName, String address, double lat, double lon, double distance,
                             int radius, String deliveryMode, long deliveryFee, boolean universalExchange,
                             long ratingSum, long ratingCount, long decided, long accepted) {
    }
}
