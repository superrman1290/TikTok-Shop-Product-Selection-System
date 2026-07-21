package com.tiktokinsight.datasource;

import java.io.InputStream;

public record ProductStatFetchRequest(InputStream content, int maxRows) {
}
