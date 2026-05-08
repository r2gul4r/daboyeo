package kr.daboyeo.backend.domain;

import java.util.Arrays;

public enum Category {
    HOT,
    MOVIE,
    PREMIERE,
    DISCOUNT;

    public static Category fromQueryValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(category -> category.name().equalsIgnoreCase(value.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("category must be one of HOT, MOVIE, PREMIERE, or DISCOUNT."));
    }
}
