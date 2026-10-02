package ci.allogaz.geo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

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
                new GeoPoint(LAT, LON), 3_000, List.of(), fixed()));

        // Fermé à cette heure-ci : seul créneau demain
        var ferme = fixtures.verifiedSeller("Fermé", LAT, LON - 0.002, 3_000, fixed());
        fixtures.offer(ferme, Fixtures.ORYX_12KG, 5_000L, null, 8);
        var tomorrow = LocalDateTime.now(ZoneId.of("Africa/Abidjan")).plusDays(1).getDayOfWeek();
        profiles.update(ferme.user().id(), new SellerProfileCommand("Fermé", null, new GeoPoint(LAT, LON - 0.002),
                3_000, List.of(new OpeningSlot(tomorrow, LocalTime.of(0, 0), LocalTime.of(23, 59))), fixed()));
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
