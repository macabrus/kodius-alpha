package com.kodius.order;

import com.kodius.order.model.Order;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.List;

public interface OrderDAO {

    @SqlQuery("SELECT * FROM service_order WHERE owner_id = ?")
    List<Order> listOrdersForUser(@Bind Integer userId);

    @SqlQuery("INSERT INTO service_order VALUES (:o.service_date, :o.model, :o.mileage)")
    Order placeOrder(@Bind("service_date") Instant serviceDate,
                     @Bind("model") String model,
                     @Bind("mileage") Integer mileage);

    @SqlQuery("DELETE FROM service_order WHERE id = ?")
    Order removeOrder(@Bind Long orderId);
}
