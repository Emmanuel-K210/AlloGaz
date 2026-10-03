package ci.allogaz.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.catalog.application.port.out.SellerOfferRepository;
import ci.allogaz.catalog.domain.DeliveryMode;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.identity.domain.Role;
import ci.allogaz.identity.domain.User;
import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.payment.application.EscrowService;
import ci.allogaz.payment.domain.LedgerAccounts;
import ci.allogaz.support.Fixtures;
import ci.allogaz.support.MutableClock;
import ci.allogaz.support.RecordingSmsSender;

/**
 * Workflow de commande de bout en bout par l'API : cas nominal, prix figé, expiration, mauvais code,
 * validation automatique, litige et commandes concurrentes sur le même stock.
 * Dépôt à Marcory (5.3030, -3.9870), livraison 500 F CFA dans un rayon de 5 km.
 */
class OrderWorkflowTest extends AbstractIntegrationTest {

    @Autowired MutableClock clock;
    @Autowired RecordingSmsSender sms;
    @Autowired EscrowService escrow;
    @Autowired OrderService orderService;
    @Autowired SellerOfferRepository offers;

    Fixtures.Seller seller;
    SellerOffer offer;
    User buyer;

    @BeforeEach
    void setUp() {
        seller = fixtures.verifiedSeller("Dépôt Marcory", 5.3030, -3.9870, 5_000,
                new DeliveryPolicy(DeliveryMode.FIXED_FEE, 500));
        offer = fixtures.offer(seller, Fixtures.ORYX_12KG, 5_200L, 26_000L, 10);
        buyer = fixtures.user();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    // ------------------------------------------------------------------ scénarios

    @Test
    void nominal_flow_from_intent_to_funds_released() throws Exception {
        String orderId = createAndSubmit(buyer, 2);
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("INTENT_SENT"))
                .andExpect(jsonPath("$.pricesFrozen").value(false));

        sellerAction("accept", orderId).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.pricesFrozen").value(true))
                .andExpect(jsonPath("$.itemsTotal").value(10_400))
                .andExpect(jsonPath("$.transportFee").value(500))
                .andExpect(jsonPath("$.total").value(10_900));

        pay(buyer, orderId).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"));
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("PAID"));
        assertThat(offers.findById(offer.id()).orElseThrow().stock()).isEqualTo(8);
        assertThat(escrow.escrowed(UUID.fromString(orderId))).isEqualTo(10_900);
        String code = sms.lastCode(buyer.phone().value());

        sellerAction("prepare", orderId).andExpect(jsonPath("$.status").value("IN_PREPARATION"));
        sellerAction("dispatch", orderId).andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));
        submitCode(orderId, code).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FUNDS_RELEASED"));

        // 10 900 - 5 % (545) = 10 355 pour le vendeur
        assertThat(escrow.escrowed(UUID.fromString(orderId))).isZero();
        assertThat(escrow.sellerBalance(seller.id())).isEqualTo(10_355);
        mvc.perform(get("/api/v1/seller/balance").header("Authorization", bearer(seller.user())))
                .andExpect(jsonPath("$.amount").value(10_355));

        mvc.perform(post("/api/v1/orders/" + orderId + "/rating").header("Authorization", bearer(buyer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stars\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.buyerRating").value(5));
    }

    @Test
    void price_is_frozen_at_acceptance_not_before() throws Exception {
        String orderId = createAndSubmit(buyer, 1);
        updateOfferPrice(5_500L);                       // avant acceptation : le nouveau prix s'applique
        sellerAction("accept", orderId).andExpect(jsonPath("$.itemsTotal").value(5_500));
        updateOfferPrice(6_000L);                       // après acceptation : sans effet
        getOrder(buyer, orderId).andExpect(jsonPath("$.itemsTotal").value(5_500)).andExpect(jsonPath("$.total").value(6_000));
    }

    @Test
    void intent_expires_when_seller_does_not_answer_in_time() throws Exception {
        String orderId = createAndSubmit(buyer, 1);
        clock.advance(Duration.ofMinutes(9));
        orderService.expireOverdueIntents();
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("INTENT_SENT"));

        clock.advance(Duration.ofMinutes(2));
        assertThat(orderService.expireOverdueIntents()).isGreaterThanOrEqualTo(1);
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("EXPIRED"));
        sellerAction("accept", orderId).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void wrong_delivery_code_is_rejected_then_locked() throws Exception {
        String orderId = paidAndDispatched(buyer);
        String code = sms.lastCode(buyer.phone().value());
        String wrong = code.equals("0000") ? "1111" : "0000";

        submitCode(orderId, wrong).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WRONG_DELIVERY_CODE"))
                .andExpect(jsonPath("$.detail").value("Code incorrect. Essais restants : 4."));
        for (int i = 0; i < 3; i++) {
            submitCode(orderId, wrong).andExpect(status().isUnprocessableContent());
        }
        submitCode(orderId, wrong).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DELIVERY_CODE_LOCKED"));
        submitCode(orderId, code).andExpect(status().isTooManyRequests());
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"))
                .andExpect(jsonPath("$.deliveryCodeAttempts").value(5));
        assertThat(escrow.escrowed(UUID.fromString(orderId))).isEqualTo(5_700);

        // L'acheteur peut toujours confirmer lui-même la réception
        mvc.perform(post("/api/v1/orders/" + orderId + "/confirm-receipt").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FUNDS_RELEASED"));
    }

    @Test
    void delivered_order_is_validated_automatically_after_the_delay() throws Exception {
        String orderId = paidAndDispatched(buyer);
        sellerAction("delivered", orderId).andExpect(jsonPath("$.status").value("DELIVERED"));

        clock.advance(Duration.ofHours(23));
        orderService.autoValidateDeliveredOrders();
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("DELIVERED"));

        clock.advance(Duration.ofHours(2));
        orderService.autoValidateDeliveredOrders();
        getOrder(buyer, orderId).andExpect(jsonPath("$.status").value("FUNDS_RELEASED"));
        assertThat(escrow.sellerBalance(seller.id())).isEqualTo(5_415);  // 5 700 - 285
    }

    @Test
    void dispute_freezes_funds_until_admin_refunds_the_buyer() throws Exception {
        String orderId = paidAndDispatched(buyer);
        sellerAction("delivered", orderId);
        mvc.perform(post("/api/v1/orders/" + orderId + "/dispute").header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Bouteille à moitié vide\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPUTED"));

        // Fonds gelés : ni validation automatique, ni code, ni confirmation
        clock.advance(Duration.ofDays(3));
        orderService.autoValidateDeliveredOrders();
        submitCode(orderId, sms.lastCode(buyer.phone().value())).andExpect(status().isUnprocessableContent());
        assertThat(escrow.escrowed(UUID.fromString(orderId))).isEqualTo(5_700);

        User admin = fixtures.user(Role.ADMIN);
        mvc.perform(get("/api/v1/admin/disputes").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=='" + orderId + "')]").exists());
        mvc.perform(post("/api/v1/admin/orders/" + orderId + "/resolve").header("Authorization", bearer(buyer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"REFUND_BUYER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/orders/" + orderId + "/resolve").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"REFUND_BUYER\",\"note\":\"Constat photo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.disputeOutcome").value("REFUND_BUYER"));

        assertThat(escrow.escrowed(UUID.fromString(orderId))).isZero();
        assertThat(escrow.sellerBalance(seller.id())).isZero();
        mvc.perform(get("/api/v1/admin/orders/" + orderId + "/ledger").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.type=='REFUND' && @.account=='" + LedgerAccounts.ESCROW + "')].amount")
                        .value(-5_700));
    }

    @Test
    void dispute_can_be_resolved_in_favour_of_the_seller() throws Exception {
        String orderId = paidAndDispatched(buyer);
        mvc.perform(post("/api/v1/orders/" + orderId + "/dispute").header("Authorization", bearer(seller.user()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Acheteur injoignable\"}"))
                .andExpect(jsonPath("$.status").value("DISPUTED"));
        mvc.perform(post("/api/v1/admin/orders/" + orderId + "/resolve").header("Authorization",
                        bearer(fixtures.user(Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"RELEASE_TO_SELLER\"}"))
                .andExpect(jsonPath("$.status").value("FUNDS_RELEASED"));
        assertThat(escrow.sellerBalance(seller.id())).isEqualTo(5_415);
    }

    @Test
    void concurrent_payments_on_last_bottle_sell_it_once_and_refund_the_other() throws Exception {
        offer = fixtures.offer(seller, Fixtures.ORYX_12KG, 5_200L, 26_000L, 1);
        User otherBuyer = fixtures.user();
        String first = createAndSubmit(buyer, 1);
        String second = createAndSubmit(otherBuyer, 1);
        sellerAction("accept", first).andExpect(status().isOk());
        sellerAction("accept", second).andExpect(status().isOk());

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> payFirst = () -> {
                start.await();
                return pay(buyer, first).andReturn().getResponse().getStatus();
            };
            Callable<Integer> paySecond = () -> {
                start.await();
                return pay(otherBuyer, second).andReturn().getResponse().getStatus();
            };
            Future<Integer> a = pool.submit(payFirst);
            Future<Integer> b = pool.submit(paySecond);
            start.countDown();
            assertThat(a.get()).isEqualTo(200);
            assertThat(b.get()).isEqualTo(200);
        }

        String s1 = JsonPath.read(getOrder(buyer, first).andReturn().getResponse().getContentAsString(), "$.status");
        String s2 = JsonPath.read(getOrder(otherBuyer, second).andReturn().getResponse().getContentAsString(), "$.status");
        assertThat(java.util.List.of(s1, s2)).containsExactlyInAnyOrder("PAID", "CANCELLED");
        assertThat(offers.findById(offer.id()).orElseThrow().stock()).isZero();
        String cancelled = s1.equals("CANCELLED") ? first : second;
        assertThat(escrow.escrowed(UUID.fromString(cancelled))).isZero();
        assertThat(escrow.entries(UUID.fromString(cancelled)))
                .anySatisfy(e -> assertThat(e.type().name()).isEqualTo("REFUND"));
    }

    @Test
    void only_participants_see_an_order_and_roles_are_enforced() throws Exception {
        String orderId = createAndSubmit(buyer, 1);
        getOrder(fixtures.user(), orderId).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/seller/orders/" + orderId + "/accept").header("Authorization", bearer(buyer)))
                .andExpect(status().isForbidden());
        Fixtures.Seller otherSeller = fixtures.verifiedSeller("Autre dépôt", 5.31, -3.99);
        mvc.perform(post("/api/v1/seller/orders/" + orderId + "/accept")
                .header("Authorization", bearer(otherSeller.user()))).andExpect(status().isNotFound());
    }

    @Test
    void delivery_outside_the_seller_radius_is_refused() throws Exception {
        // Yopougon est à plus de 5 km de Marcory
        mvc.perform(post("/api/v1/orders").header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(1, 5.3364, -4.0890)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DELIVERY_OUT_OF_RANGE"));
    }

    // ------------------------------------------------------------------ outils

    private String orderJson(int quantity, double lat, double lon) {
        return """
                {"sellerId":"%s","fulfillment":"DELIVERY","deliveryAddress":"Zone 4, Marcory",
                 "deliveryLatitude":%s,"deliveryLongitude":%s,"submit":true,
                 "lines":[{"offerId":"%s","type":"REFILL","quantity":%d}]}
                """.formatted(seller.id(), lat, lon, offer.id(), quantity);
    }

    private String createAndSubmit(User who, int quantity) throws Exception {
        String body = mvc.perform(post("/api/v1/orders").header("Authorization", bearer(who))
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(quantity, 5.2990, -3.9800)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String paidAndDispatched(User who) throws Exception {
        String orderId = createAndSubmit(who, 1);
        sellerAction("accept", orderId).andExpect(status().isOk());
        pay(who, orderId).andExpect(status().isOk());
        sellerAction("prepare", orderId).andExpect(status().isOk());
        sellerAction("dispatch", orderId).andExpect(status().isOk());
        return orderId;
    }

    private ResultActions getOrder(User who, String orderId) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/orders/" + orderId).header("Authorization", bearer(who)));
    }

    private ResultActions pay(User who, String orderId) throws Exception {
        return mvc.perform(post("/api/v1/orders/" + orderId + "/pay").header("Authorization", bearer(who)));
    }

    private ResultActions sellerAction(String action, String orderId) throws Exception {
        return mvc.perform(post("/api/v1/seller/orders/" + orderId + "/" + action)
                .header("Authorization", bearer(seller.user())));
    }

    private ResultActions submitCode(String orderId, String code) throws Exception {
        return mvc.perform(post("/api/v1/seller/orders/" + orderId + "/delivery-code")
                .header("Authorization", bearer(seller.user()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\"}"));
    }

    /** Le vendeur ne peut plus fixer le prix : seul un administrateur change le tarif national du produit. */
    private void updateOfferPrice(long refillPrice) throws Exception {
        mvc.perform(put("/api/v1/admin/products/" + Fixtures.ORYX_12KG + "/price")
                        .header("Authorization", bearer(fixtures.user(Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refillPrice\":" + refillPrice + ",\"purchasePrice\":26000}"))
                .andExpect(status().isOk());
    }
}
