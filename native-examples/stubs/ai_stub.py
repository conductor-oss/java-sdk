#!/usr/bin/env python3
"""A local stand-in for an MCP server and an A2A agent, for the kitchensink's AI section.

    python3 stubs/ai_stub.py 8765

  POST /mcp                          MCP JSON-RPC: tools/list, tools/call (get_weather, add)
  GET  /.well-known/agent-card.json  A2A agent card pointing at /a2a
  POST /a2a                          A2A JSON-RPC: message/send, answered with an agent message

Standard library only. Not a real MCP or A2A implementation: just enough of each protocol for
Conductor's LIST_MCP_TOOLS, CALL_MCP_TOOL and AGENT tasks.
"""
import json
import sys
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


def a2a(req):
    if req.get("method") != "message/send":
        return None
    message = (req.get("params") or {}).get("message") or {}
    text = " ".join(p.get("text", "") for p in message.get("parts", []) if p.get("kind") == "text")
    return {"kind": "message", "role": "agent", "messageId": str(uuid.uuid4()),
            "contextId": message.get("contextId") or str(uuid.uuid4()),
            "parts": [{"kind": "text", "text": f"Agent reply: {text.strip()} has been reviewed."}]}


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
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        req = json.loads(self.rfile.read(int(self.headers.get("Content-Length", 0))) or b"{}")
        result = mcp(req) if self.path.startswith("/mcp") else a2a(req) if self.path.startswith("/a2a") else None
        if result is None:
            self._send(200, {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32601, "message": "method not found"}})
        else:
            self._send(200, {"jsonrpc": "2.0", "id": req.get("id"), "result": result})

    def log_message(self, fmt, *args):
        sys.stderr.write("ai_stub: " + fmt % args + "\n")


if __name__ == "__main__":
    print(f"ai_stub listening on {PORT}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
