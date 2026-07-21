package com.tiktokinsight.datasource;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum SupportedMarket {
    US("USD"),
    GB("GBP"),
    TH("THB"),
    VN("VND"),
    PH("PHP"),
    MY("MYR"),
    SG("SGD"),
    ID("IDR");

    private static final Map<String, SupportedMarket> BY_CODE = Map.ofEntries(
            Map.entry("US", US), Map.entry("GB", GB), Map.entry("TH", TH), Map.entry("VN", VN),
            Map.entry("PH", PH), Map.entry("MY", MY), Map.entry("SG", SG), Map.entry("ID", ID)
    );

    private final String currency;

    SupportedMarket(String currency) {
        this.currency = currency;
    }

    public String currency() {
        return currency;
    }

    public static Optional<SupportedMarket> find(String value) {
        return value == null ? Optional.empty() : Optional.ofNullable(BY_CODE.get(value.trim().toUpperCase(Locale.ROOT)));
    }
}
