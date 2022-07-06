package com.kodius.order;

import com.kodius.order.model.Order;
import com.kodius.user.model.User;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface OrderDAO {

    @SqlQuery("SELECT * FROM service_order WHERE owner_id = ?")
    List<User> listOrdersForUser(@Bind Long userId);

    @SqlQuery("INSERT INTO service_order VALUES (:o.service_date, :o.model, :o.mileage)")
    Order placeOrder(@BindBean("o") Order order);

    @SqlQuery("DELETE FROM service_order WHERE id = ?")
    Order removeOrder(@Bind Long orderId);
}
