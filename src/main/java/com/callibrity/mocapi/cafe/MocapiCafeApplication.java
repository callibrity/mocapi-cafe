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
public class MocapiCafeApplication {

  public static void main(String[] args) {
    SpringApplication.run(MocapiCafeApplication.class, args);
  }
}
