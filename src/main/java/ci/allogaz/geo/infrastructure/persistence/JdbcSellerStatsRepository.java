package ci.allogaz.geo.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import ci.allogaz.geo.application.port.out.SellerStatsRepository;

/** Compteurs incrémentés de façon atomique (upsert), sans lecture préalable. */
@Repository
class JdbcSellerStatsRepository implements SellerStatsRepository {

    private final JdbcClient jdbc;

    JdbcSellerStatsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordDecision(UUID sellerId, boolean accepted) {
        jdbc.sql("""
                INSERT INTO seller_stats (seller_id, orders_decided, orders_accepted) VALUES (:id, 1, :accepted)
                ON CONFLICT (seller_id) DO UPDATE SET
                    orders_decided = seller_stats.orders_decided + 1,
                    orders_accepted = seller_stats.orders_accepted + EXCLUDED.orders_accepted
                """).param("id", sellerId).param("accepted", accepted ? 1 : 0).update();
    }

    @Override
    public void recordRating(UUID sellerId, int stars) {
        jdbc.sql("""
                INSERT INTO seller_stats (seller_id, rating_sum, rating_count) VALUES (:id, :stars, 1)
                ON CONFLICT (seller_id) DO UPDATE SET
                    rating_sum = seller_stats.rating_sum + EXCLUDED.rating_sum,
                    rating_count = seller_stats.rating_count + 1
                """).param("id", sellerId).param("stars", stars).update();
    }
}
