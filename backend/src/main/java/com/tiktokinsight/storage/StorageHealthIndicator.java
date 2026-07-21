package com.tiktokinsight.storage;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("storage")
public class StorageHealthIndicator implements HealthIndicator {

    private final ObjectStorage objectStorage;

    public StorageHealthIndicator(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Override
    public Health health() {
        return objectStorage.isAvailable()
                ? Health.up().build()
                : Health.down().withDetail("reason", "Local storage is unavailable").build();
    }
}
