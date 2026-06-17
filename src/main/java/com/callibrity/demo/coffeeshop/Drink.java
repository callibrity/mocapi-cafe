package com.callibrity.demo.coffeeshop;

/** A menu item. */
public record Drink(String slug, String name, int priceCents, String description) {

  public String price() {
    return "$%.2f".formatted(priceCents() / 100.0);
  }
}
