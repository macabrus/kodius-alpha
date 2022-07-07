package com.kodius.order;

import com.kodius.order.model.Order;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.LocalDate;
import java.util.List;

public interface OrderDao {

    @SqlQuery("SELECT * FROM service_order WHERE owner_id = ?")
    List<Order> listOrdersForUser(@Bind Integer userId);

    @SqlQuery("INSERT INTO service_order(owner_id, service_date, model, mileage) VALUES (:owner_id, :service_date, :model, :mileage) RETURNING *")
    Order placeOrder(@Bind("owner_id") Integer ownerId,
                     @Bind("service_date") LocalDate serviceDate,
                     @Bind("model") String model,
                     @Bind("mileage") Integer mileage);

    @SqlQuery("DELETE FROM service_order WHERE id = ?")
    Order removeOrder(@Bind Long orderId);
}
