package com.kodius.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public class MigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private Jdbi db;

    @Inject
    public MigrationRunner(Jdbi db) {
        this.db = db;
    }

    private void ensureMigrationTable(Handle h) {
        log.debug("Ensuring migration tables");
        h.useTransaction(txn -> {
            txn.execute("CREATE TABLE IF NOT EXISTS current_migration(migration_id INT)");
            txn.execute("""
                CREATE TABLE IF NOT EXISTS migration_log(
                    migration_id INT,
                    title TEXT,
                    direction TEXT,
                    description TEXT,
                    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
            """);
        });
    }

    private int currentMigration(Handle h) {
        return h.createQuery("SELECT * FROM current_migration")
            .mapTo(Integer.class)
            .findOne()
            .orElseGet(() -> {
                h.createUpdate("INSERT INTO current_migration(migration_id) VALUES (?)")
                    .bind(0, 0)
                    .execute();
                return 0;
            });
    }

    private void setCurrentMigration(Handle h, int migration) {
        h.createUpdate("UPDATE current_migration SET migration_id = ?")
            .bind(0, migration)
            .execute();
    }

    private Long getMigrationIdFromClassname(Class<?> c) {
        var parts = c.getSimpleName().split("_");
        var numPart = parts[0].substring(1);
        return Long.parseLong(numPart);
    }

    /**
     * Being at some migration means that its up() method was last executed.
     * @param id of migration to migrate to
     * @throws Exception in case of database error
     */
    public void migrate(int id) throws Exception {
        db.useTransaction(txn -> {
            ensureMigrationTable(txn);
            var current = currentMigration(txn);
            var migrations = getMigrationsInRange(Math.min(current, id), Math.max(current, id));
            if (current < id) {
                migrateUp(txn, migrations);
            }
            else if (current > id) {
                migrateDown(txn, migrations);
            }
        });
    }

    private List<Migration> getMigrationsInRange(int lower, int upper)
        throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var migrationClasses = Stream.of(getClasses("com.kodius.db.migration"))
            .filter(Migration.class::isAssignableFrom)
            .map(c -> Map.entry(getMigrationIdFromClassname(c), c))
            .filter(e -> lower <= e.getKey() && e.getKey() < upper)
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(toList());
        var migrations = new ArrayList<Migration>();
        for (var migrationClass : migrationClasses) {
            migrations.add((Migration) migrationClass.getConstructor().newInstance());
        }
        return migrations;
    }

    private void migrateUp(Handle h, List<Migration> migrations) {
        for (var migration : migrations) {
            log.debug("Running up migration %04d".formatted(migration.id()));
            h.createUpdate("INSERT INTO migration_log(migration_id, title, direction, description) VALUES (?, ?, ?, ?)")
                .bind(0, migration.id())
                .bind(1, migration.title())
                .bind(2, "UP")
                .bind(3, migration.description())
                .execute();
            migration.up(h);
        }
    }

    private void migrateDown(Handle h, List<Migration> migrations) {
        Collections.reverse(migrations);
        for (var migration : migrations) {
            log.debug("Running down migration %04d".formatted(migration.id()));
            h.createUpdate("INSERT INTO migration_log(migration_id, title, direction, description) VALUES (?, ?, ?, ?)")
                .bind(0, migration.id())
                .bind(1, migration.title())
                .bind(2, "DOWN")
                .bind(3, migration.description())
                .execute();
            migration.down(h);
        }
    }

    /**
     * Migrates database given the migration strategy.
     *
     * @param strategy for deciding which migrations should be performed
     * @throws Exception in case of database error
     */
    public void migrate(Strategy strategy) throws Exception {
        db.useTransaction(txn -> {
            ensureMigrationTable(txn);
            var current = currentMigration(txn);
            switch (strategy) {
                case LATEST -> {
                    var migrations = getMigrationsInRange(current + 1, Integer.MAX_VALUE);
                    migrateUp(txn, migrations);
                    migrations.stream()
                        .map(Migration::id)
                        .max(Integer::compareTo)
                        .ifPresent(id -> setCurrentMigration(txn, id));
                }
                case DROP -> { // data loss
                    var migrations = getMigrationsInRange(0, current + 1);
                    migrateDown(txn, migrations);
                    migrations.stream()
                        .map(Migration::id)
                        .min(Integer::compareTo)
                        .ifPresent(id -> setCurrentMigration(txn, id));
                }
                case RESET_TO_LATEST -> { // data loss
                    var downMigrations = getMigrationsInRange(0, current + 1);
                    var upMigrations = getMigrationsInRange(0, Integer.MAX_VALUE);
                    migrateDown(txn, downMigrations);
                    migrateUp(txn, upMigrations);
                    upMigrations.stream()
                        .map(Migration::id)
                        .max(Integer::compareTo)
                        .ifPresent(id -> setCurrentMigration(txn, id));
                }
            }
        });
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     * https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class[] getClasses(String packageName)
        throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}

