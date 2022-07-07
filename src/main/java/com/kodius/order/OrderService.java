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


/**
 * Service for managing Alpha shop orders
 */
public class OrderService {

    /* In real system, this should certainly be fetched live from API */
    private double USD_EUR = 0.98;

    private Jdbi db;

    @Inject
    public OrderService(Jdbi db) {
        this.db = db;
    }

    /**
     * Computes discount price for form selection
     * according to task specifications.
     *
     * @param dto form representation for which discount should be computed
     * @return discount price (form valid => result will be present)
     */
    public Optional<Integer> getDiscountPrice(OrderForm dto) {
        var maybePricing = findPricing(dto);
        if (maybePricing.isEmpty()) {
            return Optional.empty();
        }
        var pricing = maybePricing.get();
        var basePrice = 0.;

        /* Add all checkboxed services */
        if (dto.changeChain()) {
            basePrice += pricing.chainChangePrice();
        }
        if (dto.changeOilAndOilFilter()) {
            basePrice += pricing.oilAndOilFilterChangePrice();
        }
        if (dto.changeAirFilter()) {
            basePrice += pricing.airFilterChangePrice();
        }
        if (dto.changeBrakeFluid()) {
            basePrice += pricing.brakeFluidChangePrice();
        }

        /* Apply discounts */
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

        return Optional.of((int) basePrice);
    }

    /**
     * Dynamic filtering for service layer validation.
     *
     * @param dto form for which pricings should be fetched
     * @return pricing (parameter combination valid => result will be present)
     */
    public Optional<Pricing> findPricing(OrderForm dto) {
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
            query.append("AND last_supported_year <= :year ");
            params.put("year", year);
        });

        return db.withHandle(h -> {
            var pricings = h.createQuery(query.toString())
                .bindMap(params)
                .mapTo(Pricing.class)
                .list();
            return pricings.size() == 1 ? Optional.of(pricings.get(0)) : Optional.empty();
        });
    }

    /**
     * Places order into a database attaching current user
     * as owner to it.
     *
     * @param userId owner
     * @param dto submitted form
     * @return created order entity with id
     */
    public Order placeOrder(Integer userId, OrderForm dto) {
        var pricing = findPricing(dto);
        if (pricing.isEmpty()) {
            throw new BadRequestResponse();
        }

        return db.withExtension(OrderDao.class, dao ->
            dao.placeOrder(userId,
                dto.date().get(),
                dto.model().get(),
                dto.mileage().get())
        );
    }

    public List<Order> getOrdersForUser(Integer userId) {
        return db.withExtension(OrderDao.class, dao -> dao.listOrdersForUser(userId));
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

}
