package com.tiktokinsight.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageTest {

    @TempDir
    Path storageDirectory;

    private LocalObjectStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalObjectStorage(new StorageProperties(storageDirectory));
        storage.initialize();
    }

    @Test
    void storesContentUnderGeneratedObjectKey() throws Exception {
        byte[] content = "market,title\nUS,Portable Blender".getBytes(StandardCharsets.UTF_8);

        StoredObject stored = storage.put(new StorageUploadRequest(
                "../../products.csv",
                "text/csv",
                new ByteArrayInputStream(content)
        ));

        assertThat(stored.objectKey()).matches("[a-f0-9-]{36}\\.csv");
        assertThat(stored.size()).isEqualTo(content.length);
        assertThat(storage.exists(stored.objectKey())).isTrue();
        try (var input = storage.get(stored.objectKey())) {
            assertThat(input.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void blocksPathTraversalWhenReading() {
        assertThatThrownBy(() -> storage.get("../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid object key");
    }

    @Test
    void deletesStoredObject() {
        StoredObject stored = storage.put(new StorageUploadRequest(
                "payload.json",
                "application/json",
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))
        ));

        storage.delete(stored.objectKey());

        assertThat(storage.exists(stored.objectKey())).isFalse();
    }
}
