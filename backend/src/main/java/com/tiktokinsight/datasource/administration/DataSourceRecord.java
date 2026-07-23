package com.tiktokinsight.datasource.administration;

import java.time.Instant;
import java.util.List;

public record DataSourceRecord(long id, String name, String sourceType, List<String> markets, String endpointUrl,
                               String secretHint, boolean enabled, Instant createdAt, Instant updatedAt) { }
