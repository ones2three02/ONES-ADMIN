package com.ones.admin.common.web;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class CsvExportUtils {

    private CsvExportUtils() {
    }

    public static String row(Object... values) {
        return Arrays.stream(values)
                .map(CsvExportUtils::value)
                .collect(Collectors.joining(","));
    }

    public static String value(Object value) {
        if (value == null) {
            return "";
        }
        String safeValue = escapeFormula(String.valueOf(value));
        if (requiresQuote(safeValue)) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private static boolean requiresQuote(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
    }

    private static String escapeFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
            return "'" + value;
        }
        return value;
    }
}
