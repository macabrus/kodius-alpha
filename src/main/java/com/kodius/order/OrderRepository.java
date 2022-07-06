package com.kodius.order;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface OrderRepository {

    @SqlQuery("SELECT * FROM service_order WHERE owner_id = ?")
    default void listOrdersForUser(@Bind Long userId) {
    }

    @Override
    public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
        return null;
    }
}
