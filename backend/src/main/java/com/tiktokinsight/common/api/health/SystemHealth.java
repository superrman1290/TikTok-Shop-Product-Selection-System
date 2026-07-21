package com.tiktokinsight.common.api.health;

public record SystemHealth(
        ComponentStatus application,
        ComponentStatus database,
        ComponentStatus redis,
        ComponentStatus storage
) {
    public boolean allComponentsUp() {
        return application == ComponentStatus.UP
                && database == ComponentStatus.UP
                && redis == ComponentStatus.UP
                && storage == ComponentStatus.UP;
    }
}
