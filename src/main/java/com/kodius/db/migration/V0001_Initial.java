package com.kodius.db.migration;

import com.kodius.db.Migration;
import org.jdbi.v3.core.Handle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class V0001_Initial implements Migration {

    @Override
    public String description() {
        return "Initial migration which creates tables and seeds data";
    }

    @Override
    public void up(Handle db) {
        db.execute(createUserTable());
        db.execute(createOrderTable());
        db.execute(createModelTable());

        addUsers(db);
        addPricingsData(db);
    }

    @Override
    public void down(Handle db) {
        db.createScript("""
            DROP TABLE app_user CASCADE;
            DROP TABLE service_order CASCADE;
            DROP TABLE service_pricing CASCADE;
            """).execute();
    }

    private String createUserTable() {
        return """
            CREATE TABLE app_user(
                id SERIAL PRIMARY KEY,
                email TEXT UNIQUE NOT NULL
            );
            """;
    }

    private String createOrderTable() {
        return """
            CREATE TABLE service_order(
                owner_id INT,
                id SERIAL PRIMARY KEY,
                service_date TIMESTAMP,
                model TEXT,
                mileage INT,
                progress TEXT,
                FOREIGN KEY(owner_id) REFERENCES app_user(id) ON DELETE CASCADE
            );
            """;
    }

    private String createModelTable() {
        return """
            CREATE TABLE service_pricing(
                brand TEXT,
                model TEXT,
                last_supported_year INT,
                chain_change_price INT, -- cents
                oil_and_oil_filter_change_price INT, -- cents
                air_filter_change_price INT, -- cents
                brake_fluid_change_price INT -- cents
            );
            """;
    }

    private void addPricingsData(Handle db) {
        /* Read lines from CSV */
        var csv = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/service_pricing_list.csv"),
                StandardCharsets.UTF_8
            )
        ).lines().collect(Collectors.toList());

        /* Extract columns */
        var columns = Arrays.stream(csv.remove(0).split(","))
            .map(col -> col.toLowerCase().strip().replaceAll("\s+", "_"))
            .toArray(String[]::new);

        /* Map rows to hashmaps */
        var rows = csv.stream().map(line -> {
            var values = line.split(",");
            return IntStream.range(0, columns.length)
                .boxed()
                .collect(Collectors.toMap(i -> columns[i], i -> values[i]));
        }).map(row -> {
            var typedRow = new HashMap<String, Object>(row);
            /* Fix year datatype */
            for (var col : List.of("last_supported_year")) {
                typedRow.put(col, Integer.parseInt(row.get(col)));
            }
            /* Fix price datatype */
            for (var col : List.of("chain_change_price",
                                    "oil_and_oil_filter_change_price",
                                    "air_filter_change_price",
                                    "brake_fluid_change_price")
            ) {
                typedRow.put(col, Math.round(Double.parseDouble(row.get(col)) * 100));
            }
            return typedRow;
        });

        /* Insert data into the database */
        var batch = db.prepareBatch("""
            INSERT INTO service_pricing(
                brand,
                model,
                last_supported_year,
                chain_change_price,
                oil_and_oil_filter_change_price,
                air_filter_change_price,
                brake_fluid_change_price
            ) VALUES (
                :brand,
                :model,
                :last_supported_year,
                :chain_change_price,
                :oil_and_oil_filter_change_price,
                :air_filter_change_price,
                :brake_fluid_change_price
            )
            """);
        rows.forEach(row -> {
            batch.bindMap(row).add();
        });
        batch.execute();
    }

    private void addUsers(Handle db) {
        var batch = db.prepareBatch("INSERT INTO app_user(email) VALUES (?)");
        batch.add("bernard.crnkovic@icloud.com");
        batch.execute();
    }
}

