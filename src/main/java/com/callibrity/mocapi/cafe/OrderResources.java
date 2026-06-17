package com.callibrity.mocapi.cafe;

import com.callibrity.mocapi.api.resources.McpResourceTemplate;
import com.callibrity.mocapi.model.CacheScope;
import com.callibrity.mocapi.model.ReadResourceResult;
import com.callibrity.mocapi.model.ResultTypes;
import com.callibrity.mocapi.model.TextResourceContents;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RESOURCE TEMPLATE keyed by the handle returned from {@code place-order}.
 *
 * <p>Read {@code order://ORD-1001} to see that order's status. Because orders change, this is
 * marked {@code private} and non-cacheable ({@code ttlMs = 0}) — the deliberate contrast with the
 * public, cacheable menu in {@link MenuResources}.
 */
@Component
public class OrderResources {

  private final CoffeeShop shop;

  public OrderResources(CoffeeShop shop) {
    this.shop = shop;
  }

  @McpResourceTemplate(
      uriTemplate = "order://{orderId}",
      name = "Order Status",
      description = "Status of a previously placed order",
      mimeType = "text/markdown")
  public ReadResourceResult order(String orderId) {
    Order order =
        shop.findOrder(orderId)
            .orElseThrow(() -> new IllegalArgumentException("No such order: " + orderId));
    String name = shop.findDrink(order.drinkSlug()).map(Drink::name).orElse(order.drinkSlug());
    String markdown =
        """
        # Order %s

        - **Drink:** %s
        - **Size:** %s
        - **Milk:** %s
        - **Status:** %s
        """
            .formatted(order.id(), name, order.size(), order.milk(), order.status());
    return new ReadResourceResult(
        List.of(new TextResourceContents("order://" + orderId, "text/markdown", markdown)),
        0L,
        CacheScope.PRIVATE,
        ResultTypes.COMPLETE);
  }
}
