package com.kodius.order.model;

import org.immutables.value.Value;

@Value.Immutable
public interface PricingModel {
    Integer chainChangePrice();
    Integer oilAndOilFilterChangePrice();
    Integer airFilterChangePrice();
    Integer brakeFluidChangePrice();

    @Value.Auxiliary
    default Integer total() {
        return chainChangePrice() + oilAndOilFilterChangePrice() + airFilterChangePrice() + brakeFluidChangePrice();
    }
}
