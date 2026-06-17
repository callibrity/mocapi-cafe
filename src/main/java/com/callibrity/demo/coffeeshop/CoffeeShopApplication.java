package com.callibrity.demo.coffeeshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CinJUG coffee-shop MCP demo.
 *
 * <p>A single, stateless Spring Boot service built with Mocapi (MCP 2026-07-28). Because the
 * protocol is now stateless, there is no session store to externalize — so unlike the old
 * 2025-11-25 demos there are no Redis / PostgreSQL / NATS "flavors". One plain app, scaled by
 * just running more copies behind a load balancer.
 *
 * <p>Run it, then point the MCP Inspector at {@code http://localhost:8080/mcp}.
 */
@SpringBootApplication
public class CoffeeShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoffeeShopApplication.class, args);
  }
}
