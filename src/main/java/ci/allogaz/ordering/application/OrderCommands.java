package ci.allogaz.ordering.application;

import java.util.List;
import java.util.UUID;

import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.shared.domain.GeoPoint;

public final class OrderCommands {

    private OrderCommands() {
    }

    public record CreateOrder(UUID sellerId, Fulfillment fulfillment, String deliveryAddress,
                              GeoPoint deliveryLocation, List<Line> lines) {
    }

    public record Line(UUID offerId, SaleType saleType, int quantity) {
    }
}
