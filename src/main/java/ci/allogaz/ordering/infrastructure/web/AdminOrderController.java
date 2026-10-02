package ci.allogaz.ordering.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.OrderResponse;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.ResolveDisputeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration - litiges")
public class AdminOrderController {

    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/disputes")
    public List<OrderResponse> disputes() {
        return orders.disputes().stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/orders/{orderId}/resolve")
    @Operation(summary = "Tranche un litige : libération au vendeur ou remboursement de l'acheteur")
    public OrderResponse resolve(@PathVariable UUID orderId, @Valid @RequestBody ResolveDisputeRequest r) {
        return OrderResponse.from(orders.resolveDispute(orderId, r.outcome(), r.note()));
    }
}
