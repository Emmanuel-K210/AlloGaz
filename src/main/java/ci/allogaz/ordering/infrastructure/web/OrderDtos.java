package ci.allogaz.ordering.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ci.allogaz.ordering.application.OrderCommands;
import ci.allogaz.ordering.domain.DisputeOutcome;
import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.OrderLine;
import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.shared.domain.GeoPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotNull UUID sellerId,
            @NotNull Fulfillment fulfillment,
            @Size(max = 255) String deliveryAddress,
            @DecimalMin("-90") @DecimalMax("90") Double deliveryLatitude,
            @DecimalMin("-180") @DecimalMax("180") Double deliveryLongitude,
            @NotEmpty @Size(max = 10) List<@Valid LineRequest> lines,
            boolean submit) {

        OrderCommands.CreateOrder toCommand() {
            GeoPoint location = deliveryLatitude == null || deliveryLongitude == null ? null
                    : new GeoPoint(deliveryLatitude, deliveryLongitude);
            return new OrderCommands.CreateOrder(sellerId, fulfillment, deliveryAddress, location,
                    lines.stream().map(l -> new OrderCommands.Line(l.offerId(), l.type(), l.quantity())).toList());
        }
    }

    public record LineRequest(@NotNull UUID offerId, @NotNull SaleType type, @Min(1) @Max(20) int quantity) {
    }

    public record ReasonRequest(@Size(max = 500) String reason) {
    }

    public record DisputeRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record DeliveryCodeRequest(@NotBlank @Pattern(regexp = "\\d{4}", message = "4 chiffres attendus") String code) {
    }

    public record RatingRequest(@Min(1) @Max(5) int stars) {
    }

    public record ResolveDisputeRequest(@NotNull DisputeOutcome outcome, @Size(max = 500) String note) {
    }

    public record DeliveryCodeResponse(String code) {
    }

    public record LineResponse(UUID offerId, UUID productId, String productName, SaleType type, int quantity,
                               long unitPrice, long total) {

        static LineResponse from(OrderLine l) {
            return new LineResponse(l.offerId(), l.productId(), l.productName(), l.saleType(), l.quantity(),
                    l.unitPrice(), l.total());
        }
    }

    /** Montants en F CFA. pricesFrozen : vrai à partir de l'acceptation par le vendeur. */
    public record OrderResponse(UUID id, String status, UUID buyerId, UUID sellerId, Fulfillment fulfillment,
                                String deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
                                List<LineResponse> lines, long itemsTotal, long transportFee, long total,
                                boolean pricesFrozen, Instant createdAt, Instant responseDeadline,
                                Instant acceptedAt, Instant paidAt, Instant deliveredAt, Instant autoValidateAt,
                                Instant validatedAt, Instant closedAt, String statusReason, String disputeOutcome,
                                int deliveryCodeAttempts, Integer buyerRating) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.id(), o.status().name(), o.buyerId(), o.sellerId(), o.fulfillment(),
                    o.deliveryAddress(),
                    o.deliveryLocation() == null ? null : o.deliveryLocation().latitude(),
                    o.deliveryLocation() == null ? null : o.deliveryLocation().longitude(),
                    o.lines().stream().map(LineResponse::from).toList(), o.itemsTotal(), o.transportFee(), o.total(),
                    o.pricesFrozen(), o.createdAt(), o.responseDeadline(), o.acceptedAt(), o.paidAt(),
                    o.deliveredAt(), o.autoValidateAt(), o.validatedAt(), o.closedAt(), o.statusReason(),
                    o.disputeOutcome() == null ? null : o.disputeOutcome().name(), o.deliveryCodeAttempts(),
                    o.buyerRating());
        }
    }
}
