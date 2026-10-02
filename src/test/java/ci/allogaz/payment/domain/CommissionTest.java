package ci.allogaz.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class CommissionTest {

    @ParameterizedTest
    @CsvSource({"10500, 500, 525, 9975", "5210, 500, 261, 4949", "5190, 500, 260, 4930", "5700, 0, 0, 5700"})
    void splits_in_whole_francs(long total, int bps, long commission, long seller) {
        Commission.Split split = new Commission(bps).split(total);
        assertThat(split.commission()).isEqualTo(commission);
        assertThat(split.sellerAmount()).isEqualTo(seller);
        assertThat(split.commission() + split.sellerAmount()).isEqualTo(total);
    }

    @Test
    void release_transaction_is_balanced() {
        UUID order = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        LedgerTransaction tx = LedgerTransaction.release(order, seller, 10_500, new Commission(500), Instant.now());
        assertThat(tx.entries()).extracting(LedgerEntry::account, LedgerEntry::amount).containsExactly(
                org.assertj.core.groups.Tuple.tuple(LedgerAccounts.ESCROW, -10_500L),
                org.assertj.core.groups.Tuple.tuple(LedgerAccounts.seller(seller), 9_975L),
                org.assertj.core.groups.Tuple.tuple(LedgerAccounts.PLATFORM_COMMISSION, 525L));
    }

    @Test
    void nothing_to_release_from_an_empty_escrow() {
        assertThatThrownBy(() -> LedgerTransaction.refund(UUID.randomUUID(), 0, Instant.now()))
                .hasMessageContaining("Aucun montant en séquestre");
    }
}
