package ci.allogaz.ordering.application.port.out;

import java.util.UUID;

/** Mouvements du séquestre (module payment). */
public interface EscrowPort {

    /** Libère les fonds au vendeur, commission déduite ; renvoie le montant versé au vendeur. */
    long releaseToSeller(UUID orderId, UUID sellerId);

    /** Rembourse intégralement l'acheteur ; renvoie le montant remboursé. */
    long refundBuyer(UUID orderId);
}
