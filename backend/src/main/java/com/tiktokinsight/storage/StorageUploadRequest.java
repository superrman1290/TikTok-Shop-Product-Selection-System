package com.tiktokinsight.storage;

import java.io.InputStream;
import java.util.Objects;

public record StorageUploadRequest(
        String originalFilename,
        String contentType,
        InputStream content
) {
    public StorageUploadRequest {
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
