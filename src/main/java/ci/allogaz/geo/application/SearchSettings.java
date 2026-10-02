package ci.allogaz.geo.application;

import ci.allogaz.geo.domain.RankingWeights;

public record SearchSettings(double maxRadiusMeters, int candidateLimit, RankingWeights weights) {
}
