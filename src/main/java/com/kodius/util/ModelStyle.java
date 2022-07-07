package com.kodius.util;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@JsonSerialize
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Value.Style(
    deepImmutablesDetection = true,
    depluralizeDictionary = {"val:values", "index:indices"},
    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
    // init = "set*", // Builder initialization methods will have 'set' prefix
    typeAbstract = {"*Model"}, // 'Abstract' prefix will be detected and trimmed
    typeModifiable = "Mutable*",
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    // builder = "new", // construct builder using 'new' instead of factory method
    // build = "create", // rename 'build' method on builder to 'create'
    visibility = Value.Style.ImplementationVisibility.PUBLIC, // Generated class will be always public
    // Disable copy methods by default & enable hashCode caching
    defaults = @Value.Immutable(/* copy = false, */ prehash = true))
public @interface ModelStyle { }

