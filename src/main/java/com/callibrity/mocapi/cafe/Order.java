package com.callibrity.mocapi.cafe;

/** A placed order. Its {@code id} is the server-issued handle the model passes back later. */
public record Order(String id, String drinkSlug, Size size, Milk milk, String status) {}
