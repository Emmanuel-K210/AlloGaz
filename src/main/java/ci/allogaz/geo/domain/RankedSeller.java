package ci.allogaz.geo.domain;

public record RankedSeller(SellerCandidate seller, double score, double averageRating, double acceptanceRate) {
}
