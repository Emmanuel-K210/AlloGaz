package ci.allogaz.payment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ci.allogaz.shared.domain.DomainException;

/** Mouvement d'argent équilibré : construit uniquement par les fabriques ci-dessous. */
public record LedgerTransaction(UUID id, List<LedgerEntry> entries) {

    public LedgerTransaction {
        if (entries.stream().mapToLong(LedgerEntry::amount).sum() != 0) {
            throw new IllegalStateException("Transaction déséquilibrée");
        }
        entries = List.copyOf(entries);
    }

    /** Paiement de l'acheteur : Mobile Money vers séquestre. */
    public static LedgerTransaction escrowDeposit(UUID orderId, long amount, Instant now) {
        requirePositive(amount);
        UUID tx = UUID.randomUUID();
        return new LedgerTransaction(tx, List.of(
                entry(tx, orderId, LedgerAccounts.MOBILE_MONEY, EntryType.ESCROW_DEPOSIT, -amount, now),
                entry(tx, orderId, LedgerAccounts.ESCROW, EntryType.ESCROW_DEPOSIT, amount, now)));
    }

    /**
     * Libération : séquestre vers vendeur, moins la commission de la plateforme. Les frais de passerelle
     * (encaissement + reversement) sont prélevés sur cette commission, jamais sur la part du vendeur :
     * la ligne {@code COMMISSION} reflète donc la marge nette réelle de la plateforme.
     */
    public static LedgerTransaction release(UUID orderId, UUID sellerId, long escrowed, Commission commission,
            GatewayFeeRate gatewayFeeRate, Instant now) {
        requirePositive(escrowed);
        Commission.Split split = commission.split(escrowed);
        long gatewayFee = gatewayFeeRate.amountFor(escrowed);
        long netCommission = split.commission() - gatewayFee;
        UUID tx = UUID.randomUUID();
        List<LedgerEntry> entries = new ArrayList<>(List.of(
                entry(tx, orderId, LedgerAccounts.ESCROW, EntryType.ESCROW_RELEASE, -escrowed, now),
                entry(tx, orderId, LedgerAccounts.seller(sellerId), EntryType.ESCROW_RELEASE, split.sellerAmount(), now)));
        if (netCommission != 0) {
            entries.add(entry(tx, orderId, LedgerAccounts.PLATFORM_COMMISSION, EntryType.COMMISSION, netCommission, now));
        }
        if (gatewayFee != 0) {
            entries.add(entry(tx, orderId, LedgerAccounts.PLATFORM_GATEWAY_FEES, EntryType.GATEWAY_FEE, gatewayFee, now));
        }
        return new LedgerTransaction(tx, entries);
    }

    /**
     * Remboursement intégral de l'acheteur depuis le séquestre. Les frais d'encaissement déjà payés à
     * l'agrégateur ne sont pas récupérables : la plateforme les absorbe sur sa commission (qui peut donc
     * apparaître négative pour cette commande précise, faute de vente mais avec un encaissement déjà payé).
     */
    public static LedgerTransaction refund(UUID orderId, long escrowed, GatewayFeeRate gatewayFeeRate, Instant now) {
        requirePositive(escrowed);
        long gatewayFee = gatewayFeeRate.amountFor(escrowed);
        UUID tx = UUID.randomUUID();
        List<LedgerEntry> entries = new ArrayList<>(List.of(
                entry(tx, orderId, LedgerAccounts.ESCROW, EntryType.REFUND, -escrowed, now),
                entry(tx, orderId, LedgerAccounts.MOBILE_MONEY, EntryType.REFUND, escrowed, now)));
        if (gatewayFee != 0) {
            entries.add(entry(tx, orderId, LedgerAccounts.PLATFORM_GATEWAY_FEES, EntryType.GATEWAY_FEE, gatewayFee, now));
            entries.add(entry(tx, orderId, LedgerAccounts.PLATFORM_COMMISSION, EntryType.GATEWAY_FEE, -gatewayFee, now));
        }
        return new LedgerTransaction(tx, entries);
    }

    private static LedgerEntry entry(UUID tx, UUID orderId, String account, EntryType type, long amount, Instant now) {
        return new LedgerEntry(UUID.randomUUID(), tx, orderId, account, type, amount, now);
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new DomainException("NOTHING_IN_ESCROW", "Aucun montant en séquestre pour cette commande.");
        }
    }
}
