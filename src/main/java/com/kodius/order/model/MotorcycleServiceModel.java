package com.kodius.order.model;

import org.immutables.value.Value;

@Value.Immutable
public interface MotorcycleServiceModel {
    String brand();
    String model();
    Integer lastSupportedYear();
}
