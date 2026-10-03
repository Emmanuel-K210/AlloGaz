package ci.allogaz.geo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.application.SellerProfileService.SellerProfileCommand;
import ci.allogaz.catalog.domain.DeliveryMode;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.OpeningSlot;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.identity.domain.User;
import ci.allogaz.shared.domain.GeoPoint;
import ci.allogaz.support.Fixtures;

/**
 * Jeu de données autour de Yamoussoukro, loin des dépôts créés par les autres tests (Abidjan).
 * 0,0045° de latitude ≈ 500 m.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProximitySearchTest extends AbstractIntegrationTest {

    static final double LAT = 6.8276;
    static final double LON = -5.2893;
    static final String ORYX = "Oryx Energies Côte d'Ivoire";

    @Autowired
    SellerProfileService profiles;

    @BeforeAll
    void seed() {
        var proche = fixtures.verifiedSeller("Proche", LAT + 0.0045, LON, 3_000, fixed());
        fixtures.offer(proche, Fixtures.ORYX_12KG, 5_200L, null, 5);

        var rupture = fixtures.verifiedSeller("Rupture", LAT + 0.0225, LON, 5_000, fixed());
        fixtures.offer(rupture, Fixtures.ORYX_12KG, 5_100L, null, 0);

        var horsRayon = fixtures.verifiedSeller("Hors rayon", LAT + 0.108, LON, 3_000, fixed());
        fixtures.offer(horsRayon, Fixtures.ORYX_12KG, 5_000L, 26_000L, 8);

        var tropLoin = fixtures.verifiedSeller("Trop loin", LAT + 0.54, LON, 3_000, fixed());
        fixtures.offer(tropLoin, Fixtures.ORYX_12KG, 5_000L, null, 8);

        var enPause = fixtures.verifiedSeller("En pause", LAT, LON + 0.002, 3_000, fixed());
        fixtures.offer(enPause, Fixtures.ORYX_12KG, 5_000L, null, 8);
        profiles.setAcceptingOrders(enPause.user().id(), false);

        var totalSeulement = fixtures.verifiedSeller("Total seulement", LAT - 0.003, LON, 3_000, fixed());
        fixtures.offer(totalSeulement, Fixtures.TOTAL_6KG, null, 14_000L, 4);

        // Non vérifié
        User pendingUser = fixtures.user();
        SellerProfile pending = profiles.apply(pendingUser.id(), new SellerProfileCommand("Non vérifié", null,
                new GeoPoint(LAT, LON), 3_000, List.of(), fixed(), false));

        // Fermé à cette heure-ci : seul créneau demain
        var ferme = fixtures.verifiedSeller("Fermé", LAT, LON - 0.002, 3_000, fixed());
        fixtures.offer(ferme, Fixtures.ORYX_12KG, 5_000L, null, 8);
        var tomorrow = LocalDateTime.now(ZoneId.of("Africa/Abidjan")).plusDays(1).getDayOfWeek();
        profiles.update(ferme.user().id(), new SellerProfileCommand("Fermé", null, new GeoPoint(LAT, LON - 0.002),
                3_000, List.of(new OpeningSlot(tomorrow, LocalTime.of(0, 0), LocalTime.of(23, 59))), fixed(), false));
    }

    private static DeliveryPolicy fixed() {
        return new DeliveryPolicy(DeliveryMode.FIXED_FEE, 500);
    }

    @Test
    void ranks_by_score_and_keeps_far_sellers_lower() throws Exception {
        mvc.perform(get("/api/v1/search/sellers").param("lat", "" + LAT).param("lon", "" + LON)
                        .param("company", ORYX).param("sizeKg", "12.5").param("type", "REFILL"))
                .andExpect(status().isOk())
                // Hors rayon (en stock, ne livre pas jusqu'ici) passe devant Rupture (proche mais sans stock)
                .andExpect(jsonPath("$[*].shopName").value(contains("Proche", "Hors rayon", "Rupture")))
                .andExpect(jsonPath("$[0].deliversToYou").value(true))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].distanceMeters").value(org.hamcrest.Matchers.both(org.hamcrest.Matchers.greaterThan(490)).and(org.hamcrest.Matchers.lessThan(510))))
                .andExpect(jsonPath("$[0].offers[0].bottleColors").value(contains("gris", "bleu")))
                .andExpect(jsonPath("$[1].deliversToYou").value(false))
                .andExpect(jsonPath("$[2].available").value(false));
    }

    @Test
    void excludes_unverified_paused_closed_and_out_of_search_radius_sellers() throws Exception {
        mvc.perform(get("/api/v1/search/sellers").param("lat", "" + LAT).param("lon", "" + LON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].shopName").value(hasItem("Total seulement")))
                .andExpect(jsonPath("$[*].shopName").value(not(hasItem("Trop loin"))))
                .andExpect(jsonPath("$[*].shopName").value(not(hasItem("En pause"))))
                .andExpect(jsonPath("$[*].shopName").value(not(hasItem("Non vérifié"))))
                .andExpect(jsonPath("$[*].shopName").value(not(hasItem("Fermé"))));
    }

    /**
     * Isolé dans son propre coin du monde (loin de Yamoussoukro et d'Abidjan) : la requête couvre exactement
     * les mêmes paramètres (société, contenance, type) qu'une recherche « normale », donc on ne peut pas
     * réutiliser le jeu de données partagé sans fausser les autres assertions de cette classe (elles
     * dénombrent exactement leurs vendeurs attendus).
     */
    @Test
    void universal_exchange_seller_appears_for_a_different_company_than_its_own_offers() throws Exception {
        double lat = 8.5, lon = -6.5;
        var echangeUniversel = fixtures.universalExchangeSeller("Echange universel", lat - 0.001, lon, 3_000, fixed());
        fixtures.offer(echangeUniversel, Fixtures.CORLAY_6KG, 5_300L, null, 6);
        var corlaySeulement = fixtures.verifiedSeller("Corlay seulement", lat - 0.002, lon, 3_000, fixed());
        fixtures.offer(corlaySeulement, Fixtures.CORLAY_6KG, 5_000L, null, 4);

        String body = mvc.perform(get("/api/v1/search/sellers").param("lat", "" + lat).param("lon", "" + lon)
                        .param("company", ORYX).param("sizeKg", "6").param("type", "REFILL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode sellers = new ObjectMapper().readTree(body);
        List<String> names = StreamSupport.stream(sellers.spliterator(), false)
                .map(n -> n.get("shopName").asText()).toList();
        // Le point d'échange ressort même s'il ne vend que du Corlay ; le dépôt Corlay « classique » reste exclu.
        assertThat(names).contains("Echange universel").doesNotContain("Corlay seulement");

        JsonNode match = StreamSupport.stream(sellers.spliterator(), false)
                .filter(n -> n.get("shopName").asText().equals("Echange universel"))
                .findFirst().orElseThrow();
        assertThat(match.get("universalExchange").asBoolean()).isTrue();
        assertThat(match.get("offers").get(0).get("company").asText()).isEqualTo("Corlay Côte d'Ivoire");
    }

    @Test
    void filters_by_brand_and_sale_type() throws Exception {
        mvc.perform(get("/api/v1/search/sellers").param("lat", "" + LAT).param("lon", "" + LON)
                        .param("type", "PURCHASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].shopName").value(contains("Total seulement", "Hors rayon")));
        mvc.perform(get("/api/v1/search/sellers").param("lat", "" + LAT).param("lon", "" + LON)
                        .param("brand", "TotalEnergies").param("color", "Bleu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].shopName").value(contains("Total seulement")))
                .andExpect(jsonPath("$[0].offers[0].purchasePrice").value(14000));
    }
}
