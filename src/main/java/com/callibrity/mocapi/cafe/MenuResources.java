package com.callibrity.mocapi.cafe;

import com.callibrity.mocapi.api.resources.McpResource;
import com.callibrity.mocapi.api.resources.McpResourceTemplate;
import com.callibrity.mocapi.model.CacheScope;
import com.callibrity.mocapi.model.ReadResourceResult;
import com.callibrity.mocapi.model.ResultTypes;
import com.callibrity.mocapi.model.TextResourceContents;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * RESOURCES — application-controlled context. The host decides when to pull these into the model's
 * context; the model can't fetch them on its own.
 *
 * <p>The menu is public and rarely changes, so we mark it cacheable using the MCP 2026-07-28
 * caching directives ({@code ttlMs} + {@code public} {@link CacheScope}) — clients can cache it
 * instead of re-reading on every turn.
 */
@Component
public class MenuResources {

  private static final long ONE_MINUTE_MS = 60_000L;

  private final CoffeeShop shop;

  public MenuResources(CoffeeShop shop) {
    this.shop = shop;
  }

  /** Fixed-URI resource: the whole menu. */
  @McpResource(
      uri = "menu://drinks",
      name = "Menu",
      description = "The full coffee menu",
      mimeType = "text/markdown")
  public ReadResourceResult menu() {
    String markdown =
        shop.menu().stream()
            .map(d -> "- **%s** (`%s`) — %s _%s_".formatted(d.name(), d.slug(), d.description(), d.price()))
            .collect(Collectors.joining("\n", "# Menu\n\n", "\n"));
    return new ReadResourceResult(
        List.of(new TextResourceContents("menu://drinks", "text/markdown", markdown)),
        ONE_MINUTE_MS,
        CacheScope.PUBLIC,
        ResultTypes.COMPLETE);
  }

  /** RESOURCE TEMPLATE — a parameterized URI. The client fills in {@code {slug}}. */
  @McpResourceTemplate(
      uriTemplate = "menu://drinks/{slug}",
      name = "Drink",
      description = "Details for a single drink",
      mimeType = "text/markdown")
  public ReadResourceResult drink(String slug) {
    Drink drink =
        shop.findDrink(slug).orElseThrow(() -> new IllegalArgumentException("No such drink: " + slug));
    String markdown =
        "# %s\n\n%s\n\n**Price:** %s".formatted(drink.name(), drink.description(), drink.price());
    return new ReadResourceResult(
        List.of(new TextResourceContents("menu://drinks/" + slug, "text/markdown", markdown)),
        ONE_MINUTE_MS,
        CacheScope.PUBLIC,
        ResultTypes.COMPLETE);
  }
}
