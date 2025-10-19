package com.paklog.wes.pack.domain.valueobject;

import java.util.Objects;

/**
 * Shipping address value object
 */
public record Address(
        String street1,
        String street2,
        String city,
        String state,
        String zipCode,
        String country
) {
    public Address {
        Objects.requireNonNull(street1, "Street1 cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(zipCode, "Zip code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");
    }

    public Address(String street1, String city, String state, String zipCode, String country) {
        this(street1, null, city, state, zipCode, country);
    }

    public boolean isValid() {
        return street1 != null && !street1.isBlank() &&
               city != null && !city.isBlank() &&
               state != null && !state.isBlank() &&
               zipCode != null && !zipCode.isBlank() &&
               country != null && !country.isBlank();
    }

    public boolean isInternational() {
        return !"US".equalsIgnoreCase(country) && !"USA".equalsIgnoreCase(country);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street1);
        if (street2 != null && !street2.isBlank()) {
            sb.append(", ").append(street2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" ").append(zipCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}
