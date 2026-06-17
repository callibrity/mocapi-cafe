package com.callibrity.demo.coffeeshop;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * In-memory coffee shop: a fixed menu plus the orders placed so far.
 *
 * <p>Orders live in a plain map here only to keep the demo runnable with zero infrastructure. The
 * important point for the talk is the <em>handle</em>: {@link #placeOrder} returns an {@link Order}
 * whose {@code id} the caller threads back into later calls (e.g. reading {@code order://{id}}).
 * Nothing is stored in an MCP session — the protocol is stateless.
 */
@Service
public class CoffeeShop {

  private final Map<String, Drink> menu = new LinkedHashMap<>();
  private final Map<String, Order> orders = new ConcurrentHashMap<>();
  private final AtomicInteger sequence = new AtomicInteger(1000);

  public CoffeeShop() {
    add(new Drink("drip", "House Drip", 295, "Our rotating single-origin filter coffee."));
    add(new Drink("latte", "Caffe Latte", 425, "Espresso with steamed milk and a little foam."));
    add(new Drink("cold-brew", "Cold Brew", 450, "Steeped 18 hours, served over ice."));
    add(new Drink("mocha", "Mocha", 475, "Espresso, chocolate, and steamed milk."));
    add(new Drink("cortado", "Cortado", 375, "Equal parts espresso and warm milk."));
  }

  private void add(Drink drink) {
    menu.put(drink.slug(), drink);
  }

  public Collection<Drink> menu() {
    return menu.values();
  }

  public Optional<Drink> findDrink(String slug) {
    return Optional.ofNullable(menu.get(slug));
  }

  public Order placeOrder(String slug, Size size, Milk milk) {
    Drink drink =
        findDrink(slug).orElseThrow(() -> new IllegalArgumentException("No such drink: " + slug));
    String id = "ORD-" + sequence.incrementAndGet();
    Order order = new Order(id, drink.slug(), size, milk, "RECEIVED");
    orders.put(id, order);
    return order;
  }

  public Optional<Order> findOrder(String id) {
    return Optional.ofNullable(orders.get(id));
  }
}
