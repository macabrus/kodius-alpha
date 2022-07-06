package com.kodius.user.model;

import org.immutables.value.Value;

@Value.Immutable
public interface UserModel {
    String email();
}
