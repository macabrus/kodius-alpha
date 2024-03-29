package com.kodius.order.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.Optional;

@Value.Immutable
public interface OrderFormModel {
    Optional<String> brand();
    Optional<String> model();
    Optional<Integer> year();
    Optional<Integer> mileage();
    @JsonFormat(pattern = "MM/dd/yyyy")
    Optional<LocalDate> date();
    @Value.Default
    default boolean changeChain() {
        return false;
    }
    @Value.Default
    default boolean changeOilAndOilFilter() {
        return false;
    }
    @Value.Default
    default boolean changeAirFilter() {
        return false;
    }
    @Value.Default
    default boolean changeBrakeFluid() {
        return false;
    }
    @Value.Auxiliary
    default boolean fullService() {
        return changeChain() && changeOilAndOilFilter() && changeAirFilter() && changeBrakeFluid();
    }
}
