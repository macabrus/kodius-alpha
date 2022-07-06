package com.kodius.user;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface UserDAO {

    @SqlQuery("SELECT id FROM app_user WHERE email = ?")
    Integer find(@Bind String email);
}
