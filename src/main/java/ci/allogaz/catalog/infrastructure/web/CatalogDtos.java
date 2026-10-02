package ci.allogaz.catalog.infrastructure.web;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import ci.allogaz.catalog.application.SellerOfferService.OfferDetails;
import ci.allogaz.catalog.application.SellerProfileService.SellerProfileCommand;
import ci.allogaz.catalog.domain.DeliveryMode;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.OpeningSlot;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.shared.domain.GeoPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record OpeningSlotDto(@NotNull DayOfWeek day, @NotNull LocalTime opensAt, @NotNull LocalTime closesAt) {

        OpeningSlot toDomain() {
            return new OpeningSlot(day, opensAt, closesAt);
        }

        static OpeningSlotDto from(OpeningSlot s) {
            return new OpeningSlotDto(s.day(), s.opensAt(), s.closesAt());
        }
    }

    public record SellerProfileRequest(
            @NotBlank @Size(max = 120) String shopName,
            @Size(max = 255) String address,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Min(0) @Max(50_000) int deliveryRadiusMeters,
            @NotNull DeliveryMode deliveryMode,
            @PositiveOrZero long deliveryFee,
            List<@Valid OpeningSlotDto> openingHours) {

        SellerProfileCommand toCommand() {
            return new SellerProfileCommand(shopName, address, new GeoPoint(latitude, longitude), deliveryRadiusMeters,
                    openingHours == null ? List.of() : openingHours.stream().map(OpeningSlotDto::toDomain).toList(),
                    new DeliveryPolicy(deliveryMode, deliveryFee));
        }
    }

    public record SellerProfileResponse(UUID id, UUID userId, String shopName, String address, double latitude,
                                        double longitude, int deliveryRadiusMeters, DeliveryMode deliveryMode,
                                        long deliveryFee, List<OpeningSlotDto> openingHours, boolean acceptingOrders,
                                        String status, Instant createdAt) {

        static SellerProfileResponse from(SellerProfile p) {
            return new SellerProfileResponse(p.id(), p.userId(), p.shopName(), p.address(), p.location().latitude(),
                    p.location().longitude(), p.deliveryRadiusMeters(), p.deliveryPolicy().mode(),
                    p.deliveryPolicy().fixedFee(), p.openingHours().stream().map(OpeningSlotDto::from).toList(),
                    p.acceptingOrders(), p.status().name(), p.createdAt());
        }
    }

    public record OfferRequest(@Positive Long refillPrice, @Positive Long purchasePrice,
                               @PositiveOrZero int stock, Boolean active) {
    }

    public record OfferResponse(UUID id, UUID sellerId, UUID productId, String productName, String brand,
                                String company, String bottleColor, Integer capacityGrams, Long refillPrice,
                                Long purchasePrice, int stock, boolean active) {

        static OfferResponse from(OfferDetails d) {
            return new OfferResponse(d.offer().id(), d.offer().sellerId(), d.product().id(), d.product().name(),
                    d.product().brand(), d.product().company(), d.product().bottleColor(), d.product().capacityGrams(), d.offer().refillPrice(),
                    d.offer().purchasePrice(), d.offer().stock(), d.offer().active());
        }
    }

    public record PublicSellerResponse(SellerProfileResponse profile, boolean openNow, List<OfferResponse> offers) {
    }

    public record ProductRequest(@NotBlank String categorySlug, @NotBlank @Size(max = 150) String name,
                                 @Size(max = 80) String brand, @Size(max = 120) String company,
                                 @Size(max = 40) String bottleColor, @Positive Integer capacityGrams) {
    }
}
