package com.callibrity.mocapi.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Mocapi Cafe — a coffee-shop MCP demo.
 *
 * <p>A single, stateless Spring Boot service built with Mocapi (MCP 2026-07-28). Because the
 * protocol is stateless, there is no session store to stand up and no shared state between
 * requests — scale it by running more copies behind a load balancer.
 *
 * <p>Run it, then open the storefront at {@code http://localhost:8080/} (the MCP Inspector does not
 * support 2026-07-28 yet).
 */
@SpringBootApplication
public class CoffeeShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoffeeShopApplication.class, args);
  }
}
