package ci.allogaz.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.jayway.jsonpath.JsonPath;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.catalog.application.port.out.SellerOfferRepository;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.identity.domain.Role;
import ci.allogaz.identity.domain.User;
import ci.allogaz.support.Fixtures;

class SellerCatalogTest extends AbstractIntegrationTest {

    @Autowired
    SellerOfferRepository offerRepository;

    @Test
    void seller_onboarding_verification_and_offers() throws Exception {
        User user = fixtures.user();
        User admin = fixtures.user(Role.ADMIN);
        String body = """
                {"shopName":"Dépôt Riviera","address":"Riviera 2, Cocody","latitude":5.3600,"longitude":-3.9700,
                 "deliveryRadiusMeters":4000,"deliveryMode":"FIXED_FEE","deliveryFee":500,
                 "openingHours":[{"day":"MONDAY","opensAt":"07:00","closesAt":"20:00"}]}
                """;
        String created = mvc.perform(post("/api/v1/seller/profile").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.acceptingOrders").value(false))
                .andReturn().getResponse().getContentAsString();
        String sellerId = JsonPath.read(created, "$.id");

        // Le rôle SELLER apparaît dans un nouveau jeton
        User seller = userRepository.findById(user.id()).orElseThrow();
        assertThat(seller.hasRole(Role.SELLER)).isTrue();
        String sellerToken = bearer(seller);

        // Pas d'ouverture avant validation ; la fiche publique est masquée
        mvc.perform(post("/api/v1/seller/profile/open").header("Authorization", sellerToken))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("SELLER_NOT_VERIFIED"));
        mvc.perform(get("/api/v1/catalog/sellers/" + sellerId)).andExpect(status().isNotFound());

        // Seul un administrateur peut valider
        mvc.perform(post("/api/v1/admin/sellers/" + sellerId + "/verify").header("Authorization", sellerToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/sellers/" + sellerId + "/verify").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED"));

        mvc.perform(post("/api/v1/seller/profile/open").header("Authorization", sellerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptingOrders").value(true));

        mvc.perform(put("/api/v1/seller/offers/" + Fixtures.TOTAL_12KG).header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refillPrice\":5200,\"purchasePrice\":25000,\"stock\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("TotalEnergies"))
                .andExpect(jsonPath("$.capacityGrams").value(12500))
                .andExpect(jsonPath("$.company").value("TotalEnergies Marketing Côte d'Ivoire"))
                .andExpect(jsonPath("$.bottleColor").value("bleu"))
                .andExpect(jsonPath("$.refillPrice").value(5200));

        mvc.perform(get("/api/v1/catalog/sellers/" + sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.shopName").value("Dépôt Riviera"))
                .andExpect(jsonPath("$.offers.length()").value(1));
    }

    @Test
    void products_can_be_filtered_by_brand_and_size() throws Exception {
        mvc.perform(get("/api/v1/catalog/products").param("category", "gaz-butane").param("sizeKg", "12.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/catalog/products").param("brand", "oryx").param("sizeKg", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bouteille Oryx 6 kg"))
                .andExpect(jsonPath("$[0].company").value("Oryx Energies Côte d'Ivoire"))
                .andExpect(jsonPath("$[0].bottleColor").value("orange"));
        mvc.perform(get("/api/v1/catalog/products").param("company", "petro ivoire").param("color", "VERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void concurrent_stock_updates_are_detected_by_optimistic_locking() {
        Fixtures.Seller seller = fixtures.verifiedSeller("Dépôt concurrent", 5.30, -4.00);
        SellerOffer created = fixtures.offer(seller, Fixtures.ORYX_6KG, 2_000L, null, 5);

        SellerOffer first = offerRepository.findById(created.id()).orElseThrow();
        SellerOffer second = offerRepository.findById(created.id()).orElseThrow();
        first.decrementStock(1);
        second.decrementStock(1);
        offerRepository.save(first);

        assertThatThrownBy(() -> offerRepository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(offerRepository.findById(created.id()).orElseThrow().stock()).isEqualTo(4);
    }
}
