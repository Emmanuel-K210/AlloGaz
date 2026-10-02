package ci.allogaz.payment.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.payment.application.EscrowService;
import ci.allogaz.payment.application.PaymentService;
import ci.allogaz.payment.domain.LedgerEntry;
import ci.allogaz.shared.infrastructure.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@Tag(name = "Paiements")
public class PaymentController {

    private final PaymentService payments;
    private final EscrowService escrow;
    private final SellerProfileService sellers;

    public PaymentController(PaymentService payments, EscrowService escrow, SellerProfileService sellers) {
        this.payments = payments;
        this.escrow = escrow;
        this.sellers = sellers;
    }

    @PostMapping("/api/v1/payments/callback/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Notification de l'agrégateur Mobile Money (le statut est revérifié auprès de lui)")
    public void callback(@PathVariable String provider, @Valid @RequestBody Callback body) {
        payments.handleCallback(body.reference());
    }

    @GetMapping("/api/v1/seller/balance")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Solde du vendeur (fonds libérés, commission déduite), en F CFA")
    public Balance balance(@AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = sellers.mine(CurrentUser.id(jwt)).id();
        return new Balance(sellerId, escrow.sellerBalance(sellerId));
    }

    @GetMapping("/api/v1/admin/orders/{orderId}/ledger")
    @Operation(summary = "Mouvements du grand livre d'une commande")
    public List<Entry> ledger(@PathVariable UUID orderId) {
        return escrow.entries(orderId).stream().map(Entry::from).toList();
    }

    public record Callback(@NotBlank String reference) {
    }

    public record Balance(UUID sellerId, long amount) {
    }

    public record Entry(UUID transactionId, String account, String type, long amount, Instant createdAt) {

        static Entry from(LedgerEntry e) {
            return new Entry(e.transactionId(), e.account(), e.type().name(), e.amount(), e.createdAt());
        }
    }
}
