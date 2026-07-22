package com.tiktokinsight.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

final class StreamingCsvBatch<T> implements Iterable<ImportRow<T>>, AutoCloseable {

    private final CSVParser parser;
    private final int maxRows;
    private final Function<CSVRecord, T> mapper;
    private boolean iteratorCreated;

    StreamingCsvBatch(InputStream content, List<String> expectedHeaders, int maxRows, Function<CSVRecord, T> mapper) {
        this.maxRows = maxRows;
        this.mapper = mapper;
        try {
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            var reader = new InputStreamReader(withoutBom(content), decoder);
            this.parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);
            validateHeaders(expectedHeaders, parser.getHeaderNames());
        } catch (IOException | IllegalArgumentException exception) {
            throw new CsvFormatException("无法读取UTF-8 CSV", exception);
        }
    }

    @Override
    public Iterator<ImportRow<T>> iterator() {
        if (iteratorCreated) {
            throw new IllegalStateException("CSV batch can only be iterated once");
        }
        iteratorCreated = true;
        Iterator<CSVRecord> source = parser.iterator();
        return new Iterator<>() {
            private long rowsRead;

            @Override
            public boolean hasNext() {
                try {
                    return source.hasNext();
                } catch (UncheckedIOException exception) {
                    throw new CsvFormatException("CSV编码或结构无效", exception);
                }
            }

            @Override
            public ImportRow<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (++rowsRead > maxRows) {
                    throw new CsvFormatException("CSV行数超过限制");
                }
                CSVRecord record;
                try {
                    record = source.next();
                } catch (UncheckedIOException exception) {
                    throw new CsvFormatException("CSV编码或结构无效", exception);
                }
                long rowNumber = record.getRecordNumber() + 1;
                try {
                    return ImportRow.success(rowNumber, mapper.apply(record));
                } catch (RowValidationException exception) {
                    return ImportRow.failure(new ImportRowError(
                            rowNumber,
                            exception.fieldName(),
                            exception.rawValue(),
                            exception.errorCode(),
                            exception.getMessage()
                    ));
                } catch (RuntimeException exception) {
                    return ImportRow.failure(new ImportRowError(
                            rowNumber, "row", null, "INVALID_ROW", "CSV行格式无效"
                    ));
                }
            }
        };
    }

    @Override
    public void close() {
        try {
            parser.close();
        } catch (IOException exception) {
            throw new CsvFormatException("关闭CSV流失败", exception);
        }
    }

    private static void validateHeaders(List<String> expected, List<String> actual) {
        if (actual.isEmpty() || actual.stream().anyMatch(String::isBlank)) {
            throw new CsvFormatException("CSV表头为空或重复");
        }
        Set<String> expectedSet = Set.copyOf(expected);
        Set<String> actualSet = Set.copyOf(actual);
        if (expected.size() != actual.size() || !expectedSet.equals(actualSet)) {
            List<String> missing = new ArrayList<>(expectedSet);
            missing.removeAll(actualSet);
            throw new CsvFormatException("CSV表头不匹配，缺少字段: " + String.join(",", missing));
        }
    }

    private static InputStream withoutBom(InputStream content) throws IOException {
        PushbackInputStream input = new PushbackInputStream(content, 3);
        byte[] prefix = input.readNBytes(3);
        boolean bom = prefix.length == 3
                && (prefix[0] & 0xFF) == 0xEF
                && (prefix[1] & 0xFF) == 0xBB
                && (prefix[2] & 0xFF) == 0xBF;
        if (!bom && prefix.length > 0) {
            input.unread(prefix);
        }
        return input;
    }
}
