package com.callibrity.mocapi.cafe;

/** A menu item. */
public record Drink(String slug, String name, int priceCents, String description) {

  public String price() {
    return "$%.2f".formatted(priceCents() / 100.0);
  }
}
