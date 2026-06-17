/*
 * Copyright © 2026 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.callibrity.mocapi.cafe.mcp;

import com.callibrity.mocapi.api.resources.McpResourceTemplate;
import com.callibrity.mocapi.cafe.domain.Drink;
import com.callibrity.mocapi.cafe.domain.MocapiCafe;
import com.callibrity.mocapi.cafe.domain.Order;
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

  private final MocapiCafe cafe;

  public OrderResources(MocapiCafe cafe) {
    this.cafe = cafe;
  }

  @McpResourceTemplate(
      uriTemplate = "order://{orderId}",
      name = "Order Status",
      description = "Status of a previously placed order",
      mimeType = "text/markdown")
  public ReadResourceResult order(String orderId) {
    Order order =
        cafe.findOrder(orderId)
            .orElseThrow(() -> new IllegalArgumentException("No such order: " + orderId));
    String name = cafe.findDrink(order.drinkSlug()).map(Drink::name).orElse(order.drinkSlug());
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
