package com.tiktokinsight.storage;

public record StoredObject(
        String objectKey,
        long size,
        String contentType
) {
}
