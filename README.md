# The Mocapi Cafe

A tiny but complete **MCP 2026-07-28** server built with [Mocapi](https://github.com/callibrity/mocapi),
written for the "You down with MCP?" talk. It models a coffee shop and exercises every primitive the
talk covers, plus the new stateless-spec features.

Because the protocol is now **stateless**, this is a single plain Spring Boot app — no Redis/Postgres
"flavors" for session state like the old 2025-11-25 demos. Scale it by running more copies.

> **Heads up:** MCP 2026-07-28 is still a **draft** (targeted for release around July 2026). It
> isn't released in Mocapi yet, so its support currently lives on the `mcp-2026-07-28` branch (it'll
> land on `main` once the spec is final). Tooling is also catching up — notably, the public **MCP
> Inspector does not support this version yet**. Drive the demo with the bundled storefront or `curl`.

It ships with a browser storefront — **The Mocapi Cafe** — served by the app itself at
`http://localhost:8080/`. Every button is a real call to the `/mcp` endpoint, and a side-by-side
"wire log" prints the actual headers and JSON for each request and response. That's the easiest way
to drive the demo; raw `curl` (documented below) is the other.

## What each file demonstrates

| File | MCP concept | Talk slide |
|------|-------------|-----------|
| `OrderTools#placeOrder` | **Tool** — model-controlled action; returns a server-issued `orderId` (the stateless *handle*) | Tools / Stateless |
| `OrderTools#orderInteractive` | **Elicitation** via **Multi Round-Trip Requests** — pauses mid-call to ask the human, then resumes | Elicitation / MRTR |
| `MenuResources#menu` | **Resource** — app-controlled context; `public` + `ttlMs` so clients can **cache** it | Resources / Deprecations(caching) |
| `MenuResources#drink` | **Resource template** — parameterized URI `menu://drinks/{slug}` | Resources detail |
| `OrderResources#order` | **Resource template** `order://{orderId}` — reads back the handle `place-order` returned; `private`, non-cacheable | Resources / Stateless |
| `BaristaPrompts#recommend` | **Prompt** — user-controlled template (`recommend-a-drink`) | Prompts detail |

The three control models line up with the overview slide: tools = **model**-controlled, resources =
**app**-controlled, prompts = **user**-controlled.

## Prerequisites

- **JDK 25** (Mocapi targets Java 25)
- **Maven 3.9+**

## Quickstart

### 1. Build Mocapi from the `mcp-2026-07-28` branch

MCP 2026-07-28 isn't released in Mocapi yet, so its support isn't on Maven Central — it currently
lives on the **`mcp-2026-07-28`** branch (it'll land on `main` once the spec is final). Build that
branch into your local `~/.m2` first:

```bash
git clone https://github.com/callibrity/mocapi.git
cd mocapi
git checkout mcp-2026-07-28      # the unreleased 2026-07-28 work
mvn clean install -DskipTests    # publishes 0.18.0-SNAPSHOT to ~/.m2
```

This demo depends on `0.18.0-SNAPSHOT` (see the `mocapi.version` property in `pom.xml`). If Maven
reports `Could not resolve dependencies … mocapi-*:0.18.0-SNAPSHOT`, you haven't run this step or you
built a branch without the 2026-07-28 work.

### 2. Run the demo

```bash
mvn spring-boot:run
```

The app starts on port 8080: the storefront is at `http://localhost:8080/` and the stateless MCP
endpoint is at `http://localhost:8080/mcp`.

### 3. Drive it from the browser

Open **`http://localhost:8080/`**. The cafe storefront is on the left; the live wire log — styled as a
receipt printer — is on the right. Each step below is one real `/mcp` call. Watch the receipt print
the request headers, `_meta`, and the response:

1. The **menu** (top-left chalkboard) is populated by reading the `menu://drinks` **resource** on
   load — the rows you see arrived over the wire.
2. Click **details** on any drink → reads the `menu://drinks/{slug}` **resource template**; the result
   opens in a modal labeled with the resource URI it came from.
3. At the **Order Counter**, pick a drink, size, and milk, then **Place order** → calls the
   `place-order` **tool**. The ticket's `orderId` is the stateless **handle**.
4. Click **status** on a ticket → reads `order://{id}` — the same handle, read back as a resource.
5. Click **Order interactively** → triggers **MRTR elicitation**: the server pauses (a red
   **⏸ AWAITING CUSTOMER** stamp prints on the receipt), a form pops up, and answering it **resumes**
   the original call with the signed `requestState`.
6. **Ask the Barista** with a mood → renders the `recommend-a-drink` **prompt** for the model.

## Other ways to drive it

### MCP Inspector — not supported yet

The public **MCP Inspector does not speak 2026-07-28**. This protocol version is still a draft, and
the Inspector only supports the older stateful versions — it can't complete the headerless
`server/discover` + per-request `_meta` flow this server expects. Use the browser storefront or
`curl` until the Inspector ships 2026-07-28 support.

### Calling it with curl (the 2026-07-28 shape)

Streamable HTTP is now **POST-only** with required routing headers. Every request is self-contained —
no session id.

```bash
curl -sS http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2026-07-28" \
  -H "Mcp-Method: tools/call" \
  -H "Mcp-Name: place-order" \
  -d '{
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {
          "name": "place-order",
          "arguments": { "drink": "latte", "size": "MEDIUM", "milk": "OAT" },
          "_meta": {
            "io.modelcontextprotocol/protocolVersion": "2026-07-28",
            "io.modelcontextprotocol/clientInfo": { "name": "curl", "version": "1.0" },
            "io.modelcontextprotocol/clientCapabilities": {}
          }
        }
      }'
```

Three things the server enforces — easy to get wrong by hand:

- **`Accept` must list both** `application/json` and `text/event-stream`. Send only one and you get
  `-32000 Not Acceptable`.
- **`_meta` goes *inside* `params`**, not next to it. A top-level `_meta` yields
  `-32602 Missing required _meta envelope on request params`.
- **`Mcp-Name` is required on every routed method**, not just `tools/call`: it's the resource URI for
  `resources/read` (e.g. `Mcp-Name: menu://drinks`) and the prompt name for `prompts/get`.

## Notes

- **Sampling and MCP logging are gone** in 2026-07-28 (SEP-2577), so there's no `ctx.sample(...)` or
  `ctx.logger(...)` here. For LLM calls, integrate your provider's API directly; for logs, use stderr
  or OpenTelemetry (`mocapi-o11y`).
- `mocapi.mrtr.secret` is left blank, so an ephemeral signing key is generated at startup — fine for a
  single node. Set a stable secret for multi-node.
