package com.kodius.order;

import com.kodius.order.model.MotorcycleService;
import com.kodius.order.model.Order;
import com.kodius.order.model.OrderForm;
import com.kodius.order.model.Pricing;
import io.javalin.http.BadRequestResponse;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderService {

    /* In real system, this should certainly be reactively computed according to exchange rates */
    private double USD_EUR = 0.98;

    private Jdbi db;

    @Inject
    public OrderService(Jdbi db) {
        this.db = db;
    }

    public Optional<Double> getDiscountPrice(OrderForm dto) {
        if (findPricing(dto).isEmpty()) {
            return Optional.empty();
        }
        var basePrice = findPricing(dto).get().total() / 100.;
        if (dto.fullService()) {
            basePrice -= 40 / USD_EUR;
        }
        else if (dto.changeChain() && dto.changeOilAndOilFilter() && dto.changeAirFilter()) {
            basePrice *= 0.8;
        }
        else if (dto.changeOilAndOilFilter() && dto.changeAirFilter()) {
            basePrice -= 20 / USD_EUR;
        }
        else if (dto.changeChain() && dto.changeBrakeFluid()) {
            basePrice *= 0.85;
        }
        return Optional.of(basePrice);
    }

    public Optional<Pricing> findPricing(OrderForm dto) {
        return db.withHandle(h -> {
            var query = new StringBuilder("SELECT * FROM service_pricing WHERE TRUE ");
            var params = new HashMap<String, Object>();
            dto.brand().ifPresent(brand -> {
                query.append("AND brand = :brand ");
                params.put("brand", brand);
            });
            dto.model().ifPresent(model -> {
                query.append("AND model = :model ");
                params.put("model", model);
            });
            dto.year().ifPresent(year -> {
                query.append("AND year <= :year ");
                params.put("year", year);
            });
            return h.createQuery(query.toString())
                .bindMap(params)
                .mapTo(Pricing.class)
                .findOne();
        });
    }

    public List<Order> getOrdersForUser(Integer userId) {
        return db.withExtension(OrderDAO.class, dao -> dao.listOrdersForUser(userId));
    }

    public List<MotorcycleService> getAvailableServices() {
        return db.withHandle(h ->
            h.createQuery("SELECT brand, model, last_supported_year FROM service_pricing")
                .mapTo(MotorcycleService.class)
                .list()
        );
    }

    public Set<String> getSupportedBrands(List<MotorcycleService> services) {
        return services.stream()
            .map(MotorcycleService::brand)
            .collect(Collectors.toSet());
    }

    public Set<String> getSupportedModels(List<MotorcycleService> services) {
        return services.stream()
            .map(MotorcycleService::model)
            .collect(Collectors.toSet());
    }

    public void placeOrder(OrderForm dto) {
        var pricing = findPricing(dto);
        if (pricing.isEmpty()) {
            throw new BadRequestResponse();
        }
        db.useExtension(OrderDAO.class, dao ->
            dao.placeOrder(dto.date().get(),
                           dto.model().get(),
                           dto.mileage().get())
        );
        Order.builder();
        /* Dynamic constraint validation in service layer */
        db.withHandle(h -> {
            boolean isValidBrand = h.createQuery("SELECT COUNT(*) FROM service_pricing WHERE brand = ?")
                .bind(0, dto.brand())
                .mapTo(Integer.class)
                .findOne()
                .map(o -> o.compareTo(0) > 0)
                .orElse(false);
            boolean isValidModel = h.createQuery("SELECT COUNT(*) FROM service_pricing WHERE model = ?")
                .bind(0, dto.brand())
                .mapTo(Integer.class)
                .findOne()
                .map(o -> o.compareTo(0) > 0)
                .orElse(false);
            boolean
        })
    }
}
