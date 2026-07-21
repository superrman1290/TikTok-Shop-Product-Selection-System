package com.tiktokinsight.datasource;

import java.io.InputStream;

public record ProductFetchRequest(InputStream content, int maxRows) {
}
