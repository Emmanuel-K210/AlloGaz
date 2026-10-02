package ci.allogaz.geo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ci.allogaz.geo.domain.SellerCandidate.MatchingOffer;

class SellerRankingTest {

    private final SellerRanking ranking = new SellerRanking(RankingWeights.defaults());

    private static SellerCandidate candidate(String name, double distance, int radius, int stock, long ratingSum,
            long ratingCount, long decided, long accepted) {
        MatchingOffer offer = new MatchingOffer(UUID.randomUUID(), UUID.randomUUID(), "B12", "Oryx", "Oryx", List.of("gris", "bleu"), null,
                12500, 5200L, null, stock);
        return new SellerCandidate(UUID.randomUUID(), name, null, 0, 0, distance, radius, "FIXED_FEE", 500,
                ratingSum, ratingCount, decided, accepted, List.of(offer));
    }

    private List<String> names(List<SellerCandidate> candidates) {
        return ranking.rank(candidates).stream().map(r -> r.seller().shopName()).toList();
    }

    @Test
    void closer_seller_ranks_first_all_else_equal() {
        assertThat(names(List.of(candidate("loin", 4_000, 10_000, 5, 0, 0, 0, 0),
                candidate("proche", 300, 10_000, 5, 0, 0, 0, 0)))).containsExactly("proche", "loin");
    }

    @Test
    void seller_out_of_delivery_radius_is_kept_but_ranked_lower() {
        assertThat(names(List.of(candidate("hors-rayon", 1_000, 500, 5, 0, 0, 0, 0),
                candidate("livre", 1_200, 3_000, 5, 0, 0, 0, 0)))).containsExactly("livre", "hors-rayon");
    }

    @Test
    void availability_outweighs_a_small_distance_gap() {
        assertThat(names(List.of(candidate("rupture", 500, 5_000, 0, 0, 0, 0, 0),
                candidate("en-stock", 1_500, 5_000, 5, 0, 0, 0, 0)))).containsExactly("en-stock", "rupture");
    }

    @Test
    void rating_and_acceptance_break_ties_at_equal_distance() {
        assertThat(names(List.of(candidate("moyen", 1_000, 5_000, 5, 30, 10, 10, 5),
                candidate("excellent", 1_000, 5_000, 5, 48, 10, 10, 10)))).containsExactly("excellent", "moyen");
    }

    @Test
    void a_single_perfect_rating_does_not_beat_an_established_reputation() {
        RankedSeller newcomer = ranking.score(candidate("nouveau", 1_000, 5_000, 5, 5, 1, 0, 0));
        RankedSeller established = ranking.score(candidate("etabli", 1_000, 5_000, 5, 230, 50, 50, 48));
        assertThat(established.score()).isGreaterThan(newcomer.score());
        assertThat(newcomer.averageRating()).isEqualTo(5.0);
    }
}
