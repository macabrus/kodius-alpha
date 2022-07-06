package com.kodius.db.migration;

import com.kodius.db.Migration;
import org.jdbi.v3.core.Handle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class V0001_Initial implements Migration {

    @Override
    public void up(Handle db) {
        db.execute(createUserTable());
        db.execute(createOrderTable());
        db.execute(createModelTable());

        var csv = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/service_pricing_list.csv"),
                StandardCharsets.UTF_8
            )
        ).lines().collect(Collectors.toList());
        var columns = csv.remove(0).split(",");
        var rows = csv.stream().map(line -> {
            var values = line.split(",");
            return IntStream.range(0, columns.length)
                .boxed()
                .collect(Collectors.toMap(i -> columns[i], i -> values[i]));
        }).collect(Collectors.toList());
        System.out.println(rows);
    }

    @Override
    public void down(Handle db) {
        db.createScript("""
            DROP TABLE app_user CASCADE;
            DROP TABLE order CASCADE;
            DROP TABLE model CASCADE;
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
            CREATE TABLE model(
                brand TEXT,
                model TEXT,
                last_supported_year INT,
                chain_change_price INT,
                oil_and_oil_filter_change_price INT,
                air_filter_change_price INT,
                brake_fluid_change_price INT
            );
            """;
    }
}

