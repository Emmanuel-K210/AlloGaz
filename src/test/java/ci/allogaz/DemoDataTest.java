package ci.allogaz;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Les données de démonstration se chargent et donnent des résultats autour des trois quartiers. */
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
class DemoDataTest extends AbstractIntegrationTest {

    @Test
    void yopougon_buyer_finds_the_yopougon_depot_first() throws Exception {
        mvc.perform(get("/api/v1/search/sellers").param("lat", "5.3400").param("lon", "-4.0850")
                        .param("company", "Oryx Energies Côte d'Ivoire").param("sizeKg", "12.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shopName").value("Yop Gaz Service"))
                .andExpect(jsonPath("$[0].deliveryMode").value("INCLUDED"));
    }

    @Test
    void green_bottles_are_found_in_yopougon_and_marcory() throws Exception {
        mvc.perform(get("/api/v1/search/sellers").param("lat", "5.3200").param("lon", "-4.0300")
                        .param("color", "vert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].shopName").value(hasItem("Yop Gaz Service")))
                .andExpect(jsonPath("$[*].offers[*].brand").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("Corlay"))));
    }

    @Test
    void pending_depot_is_listed_for_admin_review_only() throws Exception {
        mvc.perform(get("/api/v1/catalog/sellers/5e000000-0000-0000-0000-000000000004"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/catalog/sellers/5e000000-0000-0000-0000-000000000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers.length()").value(4));
    }
}
