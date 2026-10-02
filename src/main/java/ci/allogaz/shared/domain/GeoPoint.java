package ci.allogaz.shared.domain;

/** Point WGS 84 (SRID 4326). */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (Double.isNaN(latitude) || latitude < -90 || latitude > 90
                || Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new DomainException("INVALID_COORDINATES", "Coordonnées GPS invalides.");
        }
    }

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    /** Distance orthodromique (haversine), suffisante pour vérifier un rayon de livraison. */
    public double distanceMetersTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }
}
