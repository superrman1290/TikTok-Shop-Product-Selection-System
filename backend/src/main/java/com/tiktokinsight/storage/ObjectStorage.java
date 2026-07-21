package com.tiktokinsight.storage;

import java.io.InputStream;

public interface ObjectStorage {
    StoredObject put(StorageUploadRequest request);

    InputStream get(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);

    boolean isAvailable();
}
