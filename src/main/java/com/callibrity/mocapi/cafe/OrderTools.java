package com.callibrity.mocapi.cafe;

import com.callibrity.mocapi.api.tools.McpTool;
import com.callibrity.mocapi.api.tools.McpToolContext;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

/**
 * TOOLS — model-controlled actions. The LLM decides to call these.
 *
 * <p>{@link #placeOrder} returns a server-issued {@code orderId}: the stateless "handle" the model
 * passes back on later calls (e.g. to read {@code order://{orderId}}). {@link #orderInteractive}
 * shows elicitation — under MCP 2026-07-28 a Multi Round-Trip Request: the call pauses, the client
 * collects answers from the human, and re-issues; no connection is held open.
 */
@Component
public class OrderTools {

  private final CoffeeShop shop;

  public OrderTools(CoffeeShop shop) {
    this.shop = shop;
  }

  @McpTool(name = "place-order", description = "Place an order for a drink and return an order ticket.")
  public OrderTicket placeOrder(
      @Schema(description = "Menu slug, e.g. 'latte' or 'cold-brew'") String drink,
      @Schema(description = "Cup size") Size size,
      @Schema(description = "Milk preference") Milk milk) {
    Order order = shop.placeOrder(drink, size, milk);
    return OrderTicket.of(order, shop);
  }

  @McpTool(
      name = "order-interactive",
      description = "Order a drink, asking you for any details that are missing.")
  public OrderTicket orderInteractive(McpToolContext ctx) {
    var drinkSlugs = shop.menu().stream().map(Drink::slug).toList();

    // The server pauses here and asks the human. With MRTR the handler re-runs on retry,
    // so keep pre-elicitation work cheap and idempotent.
    var answers =
        ctx.elicit(
            "What can we get started for you?",
            schema ->
                schema
                    .choose("drink", drinkSlugs)
                    .choose("size", Size.class)
                    .choose("milk", Milk.class));

    if (!answers.isAccepted()) {
      return OrderTicket.cancelled();
    }

    Order order =
        shop.placeOrder(
            answers.getChoice("drink"),
            answers.getChoice("size", Size.class),
            answers.getChoice("milk", Milk.class));
    return OrderTicket.of(order, shop);
  }

  /** What the tool hands back. {@code statusUri} is the resource-template URI for this order. */
  public record OrderTicket(String orderId, String summary, String statusUri) {

    static OrderTicket of(Order order, CoffeeShop shop) {
      String name = shop.findDrink(order.drinkSlug()).map(Drink::name).orElse(order.drinkSlug());
      String summary =
          "%s — %s, %s milk (%s)".formatted(name, order.size(), order.milk(), order.status());
      return new OrderTicket(order.id(), summary, "order://" + order.id());
    }

    static OrderTicket cancelled() {
      return new OrderTicket(null, "Order cancelled — no worries!", null);
    }
  }
}
