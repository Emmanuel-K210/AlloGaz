package ci.allogaz.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.identity.domain.User;
import ci.allogaz.ordering.application.OrderCommands;
import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.support.Fixtures;

/** Suivi temps réel : changement validé -> Redis pub/sub -> flux SSE des participants. */
class OrderEventsStreamTest extends AbstractIntegrationTest {

    @Autowired OrderService orders;

    Fixtures.Seller seller;
    SellerOffer offer;
    User buyer;

    @BeforeEach
    void setUp() {
        seller = fixtures.verifiedSeller("Dépôt Yopougon", 5.3364, -4.0890);
        offer = fixtures.offer(seller, Fixtures.ORYX_6KG, 2_100L, null, 5);
        buyer = fixtures.user();
    }

    private Order submittedOrder() {
        return orders.create(buyer.id(), new OrderCommands.CreateOrder(seller.id(), Fulfillment.PICKUP, null, null,
                List.of(new OrderCommands.Line(offer.id(), SaleType.REFILL, 1))), true);
    }

    private String token(User user) {
        return accessTokenIssuer.issue(user).value();
    }

    @Test
    void buyer_follows_an_order_through_eventsource_with_query_token() throws Exception {
        Order order = submittedOrder();
        MvcResult stream = mvc.perform(get("/api/v1/orders/" + order.id() + "/events")
                        .param("access_token", token(buyer)))
                .andExpect(request().asyncStarted()).andReturn();
        MockHttpServletResponse response = stream.getResponse();
        awaitContent(response, "event:snapshot");
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("\"to\":\"INTENT_SENT\"");

        orders.accept(seller.user().id(), order.id());

        awaitContent(response, "\"to\":\"ACCEPTED\"");
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("event:order-status", "\"from\":\"INTENT_SENT\"");
    }

    @Test
    void seller_receives_new_intents_on_personal_stream() throws Exception {
        MvcResult stream = mvc.perform(get("/api/v1/me/events").param("access_token", token(seller.user())))
                .andExpect(request().asyncStarted()).andReturn();
        awaitContent(stream.getResponse(), "connecté");

        Order order = submittedOrder();

        awaitContent(stream.getResponse(), order.id().toString());
        assertThat(stream.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("\"to\":\"INTENT_SENT\"");
    }

    @Test
    void stream_is_reserved_to_participants_and_query_token_only_works_for_streams() throws Exception {
        Order order = submittedOrder();
        mvc.perform(get("/api/v1/orders/" + order.id() + "/events").param("access_token", token(fixtures.user())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orders/" + order.id() + "/events")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/orders/" + order.id()).param("access_token", token(buyer)))
                .andExpect(status().isUnauthorized());
    }

    private static void awaitContent(MockHttpServletResponse response, String expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains(expected)) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("« " + expected + " » absent du flux : " + response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            }
            Thread.sleep(50);
        }
    }
}
