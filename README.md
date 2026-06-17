# The Mocapi Cafe

A tiny but complete **MCP 2026-07-28** server built with [Mocapi](https://github.com/callibrity/mocapi),
written for the "You down with MCP?" talk. It models a coffee shop and exercises every primitive the
talk covers, plus the new stateless-spec features.

Because the protocol is **stateless**, it's a single plain Spring Boot app: no session store to stand
up, no shared state between requests. Scale it by running more copies behind a load balancer.

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
| `OrderResources#order` | **Resource template** `order://{orderId}` — reads back the handle `place-order` returned; non-cacheable (`ttlMs 0`) | Resources / Stateless |
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

### 3. Open the storefront

Open **`http://localhost:8080/`**. The cafe storefront is on the left; the live wire log — styled as a
receipt printer — is on the right. Every action is one real `/mcp` call, and the receipt prints the
headers, `_meta`, and the response. Now take the guided tour below.

## A guided tour

Work through these in order. Each step pairs a click in the storefront with the MCP primitive behind
it and the source that implements it — so you can watch it happen on the receipt (the wire log), then
open the file and see how few lines it took. Hit **clear** on the wire log for a clean start, and keep
the window wide enough to show the storefront and receipt side by side.

Everything you see was loaded over MCP — there's no REST API behind this page. Two receipts print
before you touch anything: `discover tools` and `read menu`. The menu board itself is a *resource* the
page read on startup. The browser client that makes these calls is one file:
[`src/main/resources/static/index.html`](src/main/resources/static/index.html) — its `mcp()` helper
shows the exact headers and `_meta` envelope every request needs.

### The three control models

MCP's primitives are split by *who is in control*, and all three are on screen at once:

- **Resources** — **app**-controlled context (the menu board).
- **Tools** — **model**-controlled actions (the order counter).
- **Prompts** — **user**-controlled templates (the barista).

Keep that lens as you go; each step below is one of the three.

### 1. Resource — app-controlled context

**Click `details` on a drink** (say, Caffe Latte). A resource is context the host chooses to pull in;
the model can't reach for it on its own. It opens in a modal labeled with the URI it came from.

- **On the receipt:** `resources/read`, routed by the header `Mcp-Name: menu://drinks/latte`. The
  response carries `ttlMs` + `cacheScope: public` — the menu is cacheable, so clients needn't re-fetch
  it every turn.
- **In the code:** [`MenuResources#drink`](src/main/java/com/callibrity/mocapi/cafe/mcp/MenuResources.java)
  — a `@McpResourceTemplate` on the parameterized URI `menu://drinks/{slug}`. The whole-menu read on
  load is `MenuResources#menu` (`@McpResource`, `menu://drinks`) in the same file.

### 2. Tool — model-controlled action, and the stateless handle

**At the Order Counter pick `latte / MEDIUM / OAT` and click `Place order`.** Tools are what the model
calls to *do* something. Notice what comes back: an `orderId`. That's the **handle** — there is no
session, so the id *is* the state the client carries forward.

- **On the receipt:** `tools/call place-order`; the `structuredContent` block with `orderId` and
  `statusUri`.
- **In the code:** [`OrderTools#placeOrder`](src/main/java/com/callibrity/mocapi/cafe/mcp/OrderTools.java)
  — a `@McpTool` whose parameters become the input schema (note the `Size`/`Milk` enums surface as
  dropdowns). It returns an `OrderTicket` record; Mocapi derives the output schema from it.

### 3. The handle round-trip

**Click `status` on the ticket that just appeared.** The client hands that id back as a resource URI;
the server remembers nothing about you — it just resolves the handle.

- **On the receipt:** `resources/read order://ORD-…`. This one is **non-cacheable** (`ttlMs: 0`) — the
  deliberate opposite of the cacheable menu.
- **In the code:** [`OrderResources#order`](src/main/java/com/callibrity/mocapi/cafe/mcp/OrderResources.java)
  — a `@McpResourceTemplate` on `order://{orderId}` that looks the order up by its handle.

### 4. MRTR elicitation — the headline feature

**Click `Order interactively`.** Sometimes a tool needs more from the human mid-call. The old protocol
held a connection open and waited; 2026-07-28 does a **Multi Round-Trip Request** instead — the server
*pauses and returns*.

- **On the receipt:** `resultType: input_required`, the red **⏸ AWAITING CUSTOMER** stamp, and a
  signed `requestState` token. No socket stays open; the paused state is encoded in that token.
- **Fill the form and click `Send & resume`.** The client re-issues the same call with the signed
  state and the answers; the original tool call picks up where it left off and finishes — fully
  stateless. The receipt prints `resume MRTR (accept)` and the ticket appears.
- **In the code:** [`OrderTools#orderInteractive`](src/main/java/com/callibrity/mocapi/cafe/mcp/OrderTools.java)
  — it calls `ctx.elicit(...)` to describe the form. Read the method's comment: because the handler
  re-runs on resume, pre-elicitation work must be cheap and idempotent.

### 5. Prompt — user-controlled template

**Type a mood (e.g. `cozy`) and click `Recommend`.** Prompts are invoked by a *person* — think slash
command. The server hands back ready-made messages for the model to start from.

- **On the receipt:** `prompts/get recommend-a-drink`; the `${mood}` placeholder came back filled in.
- **In the code:** [`BaristaPrompts#recommend`](src/main/java/com/callibrity/mocapi/cafe/mcp/BaristaPrompts.java)
  — a `@McpPrompt` that compiles a `${...}` template once and renders it per call.

### Wrap-up

Stateless core, handles instead of sessions, round-trips instead of held connections — scale it by
running more copies, because there's nothing to share. Sampling and server-side logging are gone in
this draft (see [Notes](#notes)). To prove it's plain HTTP underneath, drop to a terminal and run the
`curl` example below.

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
