package com.kodius.order.model;

import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
public interface OrderModel {
    Integer id();
    Instant serviceDate();
    String model();
    Integer mileage();
}
