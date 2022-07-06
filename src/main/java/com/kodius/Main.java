package com.kodius;

import com.google.inject.Guice;
import com.kodius.db.DatabaseModule;
import com.kodius.db.MigrationRunner;
import com.kodius.db.Strategy;

public class Main {
    public static void main(String[] args) throws Exception {
        var di = Guice.createInjector(new DatabaseModule());
        var runner = di.getInstance(MigrationRunner.class);
        runner.migrate(Strategy.RESET_TO_LATEST);
//        var app = Javalin.create(config -> {
//            config.addStaticFiles("/public/",Location.CLASSPATH);
//        });
//        app.get("/", ctx -> {
//            if (ctx.sessionAttribute("email") == null)
//                ctx.redirect("/login");
//            else
//                ctx.redirect("/orders");
//        });
//        app.get("/login", ctx -> ctx.render("templates/login.peb"));
//        app.post("/login", ctx -> {
//            System.out.println(ctx.formParam("email"));
//            ctx.redirect("/orders");
//        });
//        app.get("/orders", ctx -> {
//            ctx.render("templates/orders-base.peb");
//        });
//        app.start(7000);
    }
}
