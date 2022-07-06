package com.kodius.db;

import org.jdbi.v3.core.Handle;

import java.util.Arrays;

public interface Migration {

    default Integer id() {
        var parts = getClass().getSimpleName().split("_");
        var numPart = parts[0].substring(1); // remove 'V' in prefix from first part
        return Integer.parseInt(numPart);
    }

    default String title() {
        var parts = getClass().getSimpleName().split("_");
        return String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    }

    default String description() {
        return null;
    }
    void up(Handle db);
    void down(Handle db);
}

