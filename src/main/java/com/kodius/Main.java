package com.kodius;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Guice;
import com.kodius.db.DatabaseModule;
import com.kodius.db.MigrationRunner;
import com.kodius.db.Strategy;
import com.kodius.order.OrderService;
import com.kodius.order.model.OrderForm;
import com.kodius.user.UserDao;
import io.javalin.Javalin;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.json.JavalinJackson;
import org.jdbi.v3.core.Jdbi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        /* Quick Guice DI setup */
        var di = Guice.createInjector(
            new DatabaseModule(),
            binder -> {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
                mapper.registerModule(new Jdk8Module()); // for optionals
                mapper.registerModule(new JavaTimeModule());
                binder.bind(ObjectMapper.class)
                    .toInstance(mapper);
            }
        );
        var runner = di.getInstance(MigrationRunner.class);

        runner.migrate(Strategy.LATEST);

        var app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(di.getInstance(ObjectMapper.class)));
            config.addStaticFiles(conf -> {
                conf.hostedPath = "/public";
                conf.directory = "/public";
                conf.location = Location.CLASSPATH;
            });
        });

        JavalinValidation.register(LocalDate.class, v -> {
            DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("MM/dd/yyyy")
                .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
                .toFormatter();
            return LocalDate.parse(v, fmt);
        });

        app.before(ctx -> {
            String url = ctx.path();
            if (url.startsWith("/public/")) {
                return;
            }
            boolean loggedIn = ctx.sessionAttribute("userId") != null;
            if (loggedIn && url.equals("/login")) {
                ctx.redirect("/orders");
            }
            else if (!loggedIn && !url.equals("/login")) {
                ctx.redirect("/login");
            }
        });

        app.get("/login", ctx -> ctx.render("templates/login.peb"));

        app.post("/login", ctx -> {
            var db = di.getInstance(Jdbi.class);
            var email = ctx.formParamAsClass("email", String.class);
            if (!email.hasValue()) {
                throw new BadRequestResponse();
            }
            var id = db.withExtension(UserDao.class, dao ->
                dao.find(email.get())
            );
            System.out.println("Found user of id " + id);
            if (id == null) {
                throw new NotFoundResponse();
            }
            ctx.sessionAttribute("userId", id);
            ctx.redirect("/orders");
        });

        app.get("/orders", ctx -> {
            Integer userId = ctx.sessionAttribute("userId");
            var orders = di.getInstance(OrderService.class);
            ctx.render("templates/orders-list.peb", Map.of("orders", orders.getOrdersForUser(userId)));
        });

        app.get("/orders/new", ctx -> {
            var orders = di.getInstance(OrderService.class);
            var serviceTuples = orders.getAvailableServices();
            var brands = orders.getSupportedBrands(serviceTuples);
            var models = orders.getSupportedModels(serviceTuples);
            ctx.render("templates/orders-form.peb", Map.of("brands", brands, "models", models));
        });

        // posting new order
        app.post("/orders", ctx -> {
            /* Basic data format validation on controller level */
            var dto = OrderForm.builder()
                .brand(ctx.formParamAsClass("brand", String.class).get())
                .model(ctx.formParamAsClass("model", String.class).get())
                .year(ctx.formParamAsClass("year", Integer.class).get())
                .mileage(ctx.formParamAsClass("mileage", Integer.class).get())
                .date(ctx.formParamAsClass("date", LocalDate.class).get())
                .changeChain(ctx.formParamAsClass("changeChain", Boolean.class).getOrDefault(false))
                .changeOilAndOilFilter(ctx.formParamAsClass("changeOilAndOilFilter", Boolean.class).getOrDefault(false))
                .changeAirFilter(ctx.formParamAsClass("changeAirFilter", Boolean.class).getOrDefault(false))
                .changeBrakeFluid(ctx.formParamAsClass("changeBrakeFluid", Boolean.class).getOrDefault(false))
                .build();
            System.out.println(dto);
            var orders = di.getInstance(OrderService.class);
            orders.placeOrder(ctx.sessionAttribute("userId"), dto);

            // put flash message to thank for order...
            ctx.redirect("/orders");
        });

        // handle live updates for computing form price
        app.ws("/compute-price", ws -> {
            ws.onMessage(ctx -> {
                var orders = di.getInstance(OrderService.class);
                var form = di.getInstance(ObjectMapper.class)
                    .readValue(ctx.message(), OrderForm.class);
                System.out.println(form);
                var maybePricing = orders.findPricing(form);
                maybePricing.ifPresent(System.out::println);
                maybePricing.ifPresent(pricing -> ctx.send(Map.of("base", maybePricing, "discounted", orders.getDiscountPrice(form))));
            });
        });

        app.start(7000);
    }
}
