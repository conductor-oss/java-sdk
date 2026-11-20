/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package durability;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A TCP proxy that can partition its clients from its upstreams. With several upstreams it
 * balances connections round-robin and skips an upstream that refuses, like a load balancer in
 * front of a server cluster. While partitioned, open
 * connections are reset (requests in flight fail with an indefinite outcome) and new connections
 * are accepted but never answered (requests time out), which is what a network partition looks
 * like to a client. Healing closes the held connections and forwards again.
 */
final class Proxy implements AutoCloseable {

    private final ServerSocket listener;
    private final String upstreamHost;
    private final java.util.List<Integer> upstreamPorts;
    private final java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger();
    private final Set<Socket> open = ConcurrentHashMap.newKeySet();
    private final Set<Socket> held = ConcurrentHashMap.newKeySet();
    private volatile boolean partitioned;
    private volatile boolean closed;

    Proxy(int listenPort, String upstreamHost, int upstreamPort) throws IOException {
        this(listenPort, upstreamHost, java.util.List.of(upstreamPort));
    }

    Proxy(int listenPort, String upstreamHost, java.util.List<Integer> upstreamPorts) throws IOException {
        this.listener = new ServerSocket();
        this.listener.setReuseAddress(true);
        this.listener.bind(new InetSocketAddress("127.0.0.1", listenPort));
        this.upstreamHost = upstreamHost;
        this.upstreamPorts = java.util.List.copyOf(upstreamPorts);
        Thread.ofVirtual().name("proxy-accept").start(this::acceptLoop);
    }

    void partition() {
        partitioned = true;
        for (Socket s : open) {
            reset(s);
        }
    }

    void heal() {
        partitioned = false;
        for (Socket s : held) {
            reset(s);
        }
        held.clear();
    }

    boolean isPartitioned() {
        return partitioned;
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket client = listener.accept();
                if (partitioned) {
                    held.add(client); // never answered: the client's request times out
                    continue;
                }
                Socket server = connectUpstream();
                if (server == null) {
                    reset(client); // every upstream down: the client sees a reset
                    continue;
                }
                open.add(client);
                open.add(server);
                Thread.ofVirtual().start(() -> pump(client, server));
                Thread.ofVirtual().start(() -> pump(server, client));
            } catch (IOException e) {
                if (!closed) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
        }
    }

    /** The next upstream that accepts a connection, round-robin; null when none does. */
    private Socket connectUpstream() {
        int start = Math.floorMod(next.getAndIncrement(), upstreamPorts.size());
        for (int i = 0; i < upstreamPorts.size(); i++) {
            int port = upstreamPorts.get((start + i) % upstreamPorts.size());
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(upstreamHost, port), 1000);
                return s;
            } catch (IOException ignored) {
                // try the next upstream
            }
        }
        return null;
    }

    private void pump(Socket from, Socket to) {
        byte[] buffer = new byte[16 * 1024];
        try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (partitioned) {
                    break;
                }
                out.write(buffer, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
            // connection reset by the other side or by a partition
        } finally {
            reset(from);
            reset(to);
        }
    }

    private void reset(Socket s) {
        open.remove(s);
        try {
            s.setSoLinger(true, 0); // RST, not FIN
        } catch (IOException ignored) {
        }
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        heal();
        for (Socket s : open) {
            reset(s);
        }
        listener.close();
    }
}
