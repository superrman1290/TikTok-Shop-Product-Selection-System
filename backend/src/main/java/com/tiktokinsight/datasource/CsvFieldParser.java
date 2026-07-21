package com.tiktokinsight.datasource;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.apache.commons.csv.CSVRecord;

final class CsvFieldParser {

    private CsvFieldParser() {
    }

    static String required(CSVRecord record, String field, int maxLength) {
        String value = value(record, field);
        if (value.isBlank()) {
            throw invalid(field, value, "REQUIRED", "字段不能为空");
        }
        if (value.length() > maxLength) {
            throw invalid(field, value, "TOO_LONG", "字段长度超过限制");
        }
        return value;
    }

    static String optional(CSVRecord record, String field, int maxLength) {
        String value = value(record, field);
        if (value.length() > maxLength) {
            throw invalid(field, value, "TOO_LONG", "字段长度超过限制");
        }
        return value.isBlank() ? null : value;
    }

    static String url(CSVRecord record, String field) {
        String value = optional(record, field, 1500);
        if (value == null) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw invalid(field, value, "INVALID_URL", "只允许 http 或 https URL");
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw invalid(field, value, "INVALID_URL", "URL格式无效");
        }
    }

    static long nonNegativeLong(CSVRecord record, String field) {
        String value = required(record, field, 30);
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(field, value, "INVALID_INTEGER", "必须是非负整数");
        }
    }

    static BigDecimal money(CSVRecord record, String field, boolean required) {
        return decimal(record, field, required, BigDecimal.ZERO, null, "INVALID_MONEY");
    }

    static BigDecimal rating(CSVRecord record, String field, boolean required) {
        return decimal(record, field, required, BigDecimal.ZERO, new BigDecimal("5"), "INVALID_RATING");
    }

    static BigDecimal percentage(CSVRecord record, String field) {
        return decimal(record, field, false, BigDecimal.ZERO, new BigDecimal("100"), "INVALID_PERCENTAGE");
    }

    static Instant instant(CSVRecord record, String field, boolean required) {
        String value = required ? required(record, field, 40) : optional(record, field, 40);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid(field, value, "INVALID_TIMESTAMP", "时间必须是 ISO 8601 UTC 格式");
        }
    }

    static LocalDate date(CSVRecord record, String field) {
        String value = required(record, field, 20);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid(field, value, "INVALID_DATE", "日期必须是 yyyy-MM-dd 格式");
        }
    }

    static String platform(CSVRecord record) {
        String value = required(record, "platform", 40).toUpperCase();
        if (!"TIKTOK_SHOP".equals(value)) {
            throw invalid("platform", value, "UNSUPPORTED_PLATFORM", "只支持 TIKTOK_SHOP");
        }
        return value;
    }

    static SupportedMarket market(CSVRecord record) {
        String value = required(record, "market", 10);
        return SupportedMarket.find(value).orElseThrow(
                () -> invalid("market", value, "UNSUPPORTED_MARKET", "不支持的市场")
        );
    }

    static String currency(CSVRecord record, SupportedMarket market) {
        String value = required(record, "currency", 3).toUpperCase();
        if (!market.currency().equals(value)) {
            throw invalid("currency", value, "CURRENCY_MISMATCH", "货币与市场不匹配");
        }
        return value;
    }

    private static BigDecimal decimal(
            CSVRecord record,
            String field,
            boolean required,
            BigDecimal minimum,
            BigDecimal maximum,
            String errorCode
    ) {
        String value = required ? required(record, field, 40) : optional(record, field, 40);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(minimum) < 0 || maximum != null && parsed.compareTo(maximum) > 0) {
                throw new NumberFormatException("outside range");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(field, value, errorCode, "数值格式或范围无效");
        }
    }

    private static String value(CSVRecord record, String field) {
        try {
            String value = record.get(field);
            return value == null ? "" : value.trim();
        } catch (IllegalArgumentException exception) {
            throw invalid(field, null, "MISSING_COLUMN", "缺少CSV字段");
        }
    }

    private static RowValidationException invalid(String field, String value, String code, String message) {
        return new RowValidationException(field, sanitize(field, value), code, message);
    }

    private static String sanitize(String field, String value) {
        String normalized = field.toLowerCase();
        if (normalized.contains("token") || normalized.contains("password")
                || normalized.contains("secret") || normalized.contains("key")) {
            return "[REDACTED]";
        }
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
