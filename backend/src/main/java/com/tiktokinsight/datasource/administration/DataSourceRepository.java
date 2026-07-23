package com.tiktokinsight.datasource.administration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DataSourceRepository {
    List<DataSourceRecord> findAll(); Optional<DataSourceRecord> find(long id);
    DataSourceRecord create(DataSourceRecord record, String encryptedSecret, long userId, Instant now);
    DataSourceRecord update(long id, DataSourceRecord record, String encryptedSecret, long userId, Instant now);
}
