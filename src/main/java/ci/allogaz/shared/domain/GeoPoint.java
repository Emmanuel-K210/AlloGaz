package ci.allogaz.shared.domain;

/** Point WGS 84 (SRID 4326). */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (Double.isNaN(latitude) || latitude < -90 || latitude > 90
                || Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new DomainException("INVALID_COORDINATES", "Coordonnées GPS invalides.");
        }
    }
}
