package com.tiktokinsight.product.domain;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public enum ProductSort {
    COLLECTED_AT("p.collected_at"),
    LISTED_AT("p.listed_at"),
    CURRENT_PRICE("p.current_price"),
    RATING("p.rating"),
    REVIEW_COUNT("p.review_count"),
    TITLE("p.title");

    private final String column;

    ProductSort(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }

    public static ProductSort fromApiValue(String value) {
        try {
            return valueOf(value.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.UNSUPPORTED_SORT_FIELD);
        }
    }
}
