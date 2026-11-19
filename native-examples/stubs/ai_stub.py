#!/usr/bin/env python3
"""A local stand-in for an MCP server, A2A agents, a chat model and a small HTTP API, for the
examples and the kitchensink.

    python3 stubs/ai_stub.py 8765

  POST /mcp                          MCP JSON-RPC: tools/list, tools/call (get_weather, add)
  GET  /.well-known/agent-card.json  A2A agent card pointing at /a2a
  POST /a2a, /a2a/<name>             A2A JSON-RPC: message/send, tasks/get, tasks/cancel; an agent at
                                     /a2a/<name> says its name in its replies
  POST /v1/chat/completions          OpenAI-compatible chat, for the server's LiteLLM provider
                                     (conductor.ai.litellm.baseURL=http://localhost:8765):
                                     model support-router answers a ticket with a routing decision
                                     as JSON, by keywords; support-writer drafts a customer reply;
                                     planner answers the agent harness's next step as JSON;
                                     any other model summarizes what it was sent
  GET  /orders/<id>                  an order, as JSON (the HTTP example)
  POST /shipments                    creates a shipment for the order in the body; echoes the
                                     X-Request-Id header

A message is answered at once with an agent message, unless its text holds a marker:

  [slow N] a task that is "working" for N seconds (6 if no N), then completed. When the request
           asks for push notifications, the stub calls the given URL back once it is done, with
           the token as a Bearer header, as an A2A agent does
  [ask]    a task that asks for input ("input-required"); a message to that task completes it
  [fail]   a task that failed
  [data]   a completed task whose artifact holds a data part (structured output)

A message in a known context gets the number of its turn; an Authorization header is echoed.

Standard library only. Not a real MCP or A2A implementation: just enough of each protocol for
Conductor's LIST_MCP_TOOLS, CALL_MCP_TOOL and AGENT tasks.
"""
import json
import re
import sys
import time
import urllib.request
import threading
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765

TOOLS = [
    {"name": "get_weather", "description": "Current weather for a city",
     "inputSchema": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}},
    {"name": "add", "description": "Adds two numbers",
     "inputSchema": {"type": "object", "properties": {"a": {"type": "number"}, "b": {"type": "number"}}}},
]


def mcp(req):
    method, params = req.get("method"), req.get("params") or {}
    if method == "tools/list":
        return {"tools": TOOLS}
    if method == "tools/call":
        name, args = params.get("name"), params.get("arguments") or {}
        if name == "get_weather":
            text = f"Sunny and 18C in {args.get('city', 'somewhere')}"
        elif name == "add":
            text = str(args.get("a", 0) + args.get("b", 0))
        else:
            return {"content": [{"type": "text", "text": f"unknown tool {name}"}], "isError": True}
        return {"content": [{"type": "text", "text": text}], "isError": False}
    if method == "initialize":
        return {"protocolVersion": "2025-03-26", "capabilities": {"tools": {}},
                "serverInfo": {"name": "kitchensink-stub", "version": "1"}}
    return None


TASKS = {}     # task id -> task
TURNS = {}     # context id -> messages seen
LOCK = threading.Lock()


def text_part(text):
    return {"kind": "text", "text": text}


def task(context, state, *, message=None, artifacts=None):
    t = {"kind": "task", "id": str(uuid.uuid4()), "contextId": context, "status": {"state": state}}
    if message:
        t["status"]["message"] = {"kind": "message", "role": "agent", "messageId": str(uuid.uuid4()),
                                  "parts": [text_part(message)]}
    if artifacts:
        t["artifacts"] = artifacts
    TASKS[t["id"]] = t
    return t


def finish_if_due(t):
    if t["status"]["state"] == "working" and time.time() >= t.get("_due", 0):
        t["status"] = {"state": "completed"}
        t["artifacts"] = [{"artifactId": "report", "parts": [text_part(f"Done: {t['_result']}")]}]


def push_when_done(task_id, push):
    """Tells the caller the task changed, as an A2A agent's push notification does."""
    with LOCK:
        t = TASKS[task_id]
        finish_if_due(t)
        body = json.dumps({k: v for k, v in t.items() if not k.startswith("_")}).encode()
    request = urllib.request.Request(push["url"], data=body, method="POST", headers={
        "Content-Type": "application/json", "Authorization": f"Bearer {push.get('token', '')}"})
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            sys.stderr.write(f"ai_stub: pushed {task_id}: {response.status}\n")
    except Exception as e:
        sys.stderr.write(f"ai_stub: push for {task_id} failed: {e}\n")


ROUTES = [
    ("billing", ("refund", "charge", "invoice", "billing", "payment", "paid")),
    ("technical", ("error", "crash", "login", "password", "bug", "app")),
    ("shipping", ("delivery", "shipping", "package", "tracking", "arrive")),
]


def text_of(content):
    if isinstance(content, list):
        return " ".join(p.get("text", "") for p in content if isinstance(p, dict))
    return content or ""


def plan(prompt):
    """The agent harness's planner: one step at a time, decided by what has been observed so far.
    A real model decides freely; this one follows a script so that a run is repeatable."""
    found = re.search(r"\bin ([A-Z][a-z]+)", prompt)
    city = found.group(1) if found else "Paris"
    if "tool get_weather:" not in prompt:
        return {"action": "tool", "tool": "get_weather", "args": {"city": city}}
    if "delegated:" not in prompt:
        return {"action": "delegate", "task": f"[slow 4] Find three museums in {city} open tomorrow"}
    if "tool add:" not in prompt:
        return {"action": "tool", "tool": "add", "args": {"a": 12, "b": 18}}
    if "person answered:" not in prompt:
        return {"action": "ask_human", "question": f"Book two museum tickets in {city} for 30 EUR?"}
    seen = prompt.split("Observations:", 1)[-1].strip().splitlines()
    seen = [line.strip() for line in seen if line.strip() and not line.strip().startswith("Formatting instruction")]
    return {"action": "finish", "answer": f"Plan for {city}: " + "; ".join(seen)}


SHIPMENTS = []


def chat(req):
    """An OpenAI chat completion: a stand-in model that routes support tickets or drafts replies."""
    messages = req.get("messages") or []
    user = " ".join(text_of(m.get("content")) for m in messages if m.get("role") == "user")
    if "router" in (req.get("model") or ""):
        lowered = user.lower()
        team = next((t for t, words in ROUTES if any(w in lowered for w in words)), "human")
        word = next((w for t, words in ROUTES if t == team for w in words if w in lowered), None)
        reason = f"the ticket mentions '{word}'" if word else "the ticket fits no team"
        content = json.dumps({"team": team, "reason": reason, "urgent": "urgent" in lowered or "asap" in lowered})
    elif "planner" in (req.get("model") or ""):
        content = json.dumps(plan(user))
    elif "support-writer" in (req.get("model") or ""):
        content = f"Dear customer, thank you for reaching out. {user.strip()} Kind regards, Support"
    else:
        content = f"In short: {user.strip()[:120]}"
    return {"id": "chatcmpl-" + uuid.uuid4().hex[:12], "object": "chat.completion", "model": req.get("model"),
            "choices": [{"index": 0, "message": {"role": "assistant", "content": content}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": len(user.split()), "completion_tokens": len(content.split()),
                      "total_tokens": len(user.split()) + len(content.split())}}


def a2a(req, headers, agent=None):
    method, params = req.get("method"), req.get("params") or {}
    with LOCK:
        if method == "tasks/get":
            t = TASKS.get(params.get("id"))
            if t:
                finish_if_due(t)
            return {k: v for k, v in t.items() if not k.startswith("_")} if t else None
        if method == "tasks/cancel":
            t = TASKS.get(params.get("id"))
            if t:
                t["status"] = {"state": "canceled"}
            return t
        if method != "message/send":
            return None
        message = params.get("message") or {}
        text = " ".join(p.get("text", "") for p in message.get("parts", []) if p.get("kind") == "text").strip()
        known = message.get("contextId") in TURNS
        context = message.get("contextId") or str(uuid.uuid4())
        TURNS[context] = TURNS.get(context, 0) + 1
        waiting = TASKS.get(message.get("taskId") or "")
        if waiting and waiting["status"]["state"] == "input-required":
            waiting["status"] = {"state": "completed"}
            waiting["artifacts"] = [{"artifactId": "answer", "parts": [text_part(f"Done, as you said: {text}")]}]
            return waiting
        slow = re.search(r"\[slow(?: (\d+))?\]", text)
        if slow:
            t = task(context, "working")
            t["_due"] = time.time() + int(slow.group(1) or 6)
            t["_result"] = re.sub(r"\[slow[^]]*\]\s*", "", text)
            push = (params.get("configuration") or {}).get("pushNotificationConfig")
            if push:
                threading.Timer(t["_due"] - time.time() + 0.2, push_when_done, (t["id"], push)).start()
            return {k: v for k, v in t.items() if not k.startswith("_")}
        if "[ask]" in text:
            return task(context, "input-required", message="Refund to the card, or as store credit?")
        if "[fail]" in text:
            return task(context, "failed", message="The billing system is unreachable.")
        if "[data]" in text:
            return task(context, "completed", artifacts=[{"artifactId": "triage", "parts": [
                text_part("Priority high, for billing."), {"kind": "data", "data": {"priority": "high", "team": "billing"}}]}])
        reply = f"Agent reply: {text} has been reviewed."
        if agent:
            reply = f"{agent} agent: {text} - handled."
        if known:
            reply = f"Agent reply: {text} (turn {TURNS[context]} of this conversation)."
        if headers.get("Authorization"):
            reply += f" Authorized as {headers['Authorization'].split()[-1]}."
        return {"kind": "message", "role": "agent", "messageId": str(uuid.uuid4()), "contextId": context,
                "parts": [text_part(reply)]}


class Handler(BaseHTTPRequestHandler):
    def _send(self, status, body):
        data = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path.startswith("/.well-known/agent"):
            self._send(200, {"name": "kitchensink-agent", "description": "Stub support agent",
                             "url": f"http://localhost:{PORT}/a2a", "version": "1.0.0",
                             "protocolVersion": "0.3.0", "capabilities": {"streaming": False},
                             "defaultInputModes": ["text"], "defaultOutputModes": ["text"], "skills": []})
        elif self.path.startswith("/orders/"):
            order = self.path[len("/orders/"):].strip("/")
            self._send(200, {"id": order, "customer": "ana", "status": "paid", "total": 42.5,
                             "items": [{"sku": "mug", "qty": 2}, {"sku": "tea", "qty": 1}]})
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        req = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        if self.path.startswith("/v1/chat/completions"):
            self._send(200, chat(req))
            return
        if self.path.startswith("/shipments"):
            with LOCK:
                SHIPMENTS.append(req)
                number = len(SHIPMENTS)
            self._send(201, {"shipmentId": f"S-{number}", "orderId": req.get("orderId"), "carrier": "dhl",
                             "requestId": self.headers.get("X-Request-Id")})
            return
        agent = None
        if self.path.startswith("/a2a/"):
            agent = self.path[len("/a2a/"):].strip("/") or None
        result = mcp(req) if self.path.startswith("/mcp") else a2a(req, self.headers, agent) if self.path.startswith("/a2a") else None
        if result is None:
            self._send(200, {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32601, "message": "method not found"}})
        else:
            self._send(200, {"jsonrpc": "2.0", "id": req.get("id"), "result": result})

    def log_message(self, fmt, *args):
        sys.stderr.write("ai_stub: " + fmt % args + "\n")


if __name__ == "__main__":
    print(f"ai_stub listening on {PORT}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
