package durability.independent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts a bench run's history to a Jepsen history of list-append transactions and checks it
 * with Elle. A deposit signal {@code dep-N} sent to execution {@code k} is {@code [:append k N]};
 * a query answer is {@code [:r k [...]]}. Elle knows nothing about Conductor or this bench's
 * checker: it is an independent check of the same history.
 *
 * <p>Checked under {@code :serializable} (every read is consistent with one order of appends; no
 * aborted, intermediate or duplicate values; no cycles) and {@code :strict-serializable} (the
 * same, in real-time order). Native workflow queries read applied state, which may lag behind a
 * signal's acknowledgement, so strict serializability is not expected to hold.
 */
public final class ElleCheck {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        List<Map<String, Object>> history = new ArrayList<>();
        for (String line : Files.readAllLines(dir.resolve("history.jsonl"))) {
            if (!line.isBlank()) {
                history.add(JSON.readValue(line, Map.class));
            }
        }
        Files.writeString(dir.resolve("elle-history.edn"), toEdn(history, false));
        // only the signals and the queries made after every execution ended: by then everything
        // acknowledged was applied, so real-time order must hold, and a lost signal violates it
        Files.writeString(dir.resolve("elle-final.edn"), toEdn(history, true));

        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("elle.list-append"));
        require.invoke(Clojure.read("jepsen.history"));
        IFn loadString = Clojure.var("clojure.core", "load-string");
        Object result = loadString.invoke("""
                (let [load    (fn [f] (jepsen.history/history (clojure.edn/read-string (slurp f))))
                      all     (load "%s")
                      fin     (load "%s")
                      check   (fn [h model] (elle.list-append/check {:consistency-models [model]} h))
                      summary (fn [r] {:valid? (:valid? r)
                                       :anomaly-types (:anomaly-types r)
                                       :example (let [e (some-> r :anomalies first val first str)]
                                                  (when e (subs e 0 (min 600 (count e)))))})]
                  {:ops (count all)
                   :all-ops-serializable (summary (check all :serializable))
                   :all-ops-strict-serializable (summary (check all :strict-serializable))
                   :signals-and-final-reads-strict-serializable (summary (check fin :strict-serializable))})
                """.formatted(path(dir, "elle-history.edn"), path(dir, "elle-final.edn")));
        String text = String.valueOf(result);
        Files.writeString(dir.resolve("elle-result.edn"), text);
        System.out.println(text);
        System.exit(0);
    }

    private static String path(Path dir, String file) {
        return dir.resolve(file).toAbsolutePath().toString().replace("\\", "\\\\");
    }

    /** The bench history as Jepsen ops; a process that saw an indefinite result is retired. */
    @SuppressWarnings("unchecked")
    static String toEdn(List<Map<String, Object>> history, boolean finalReadsOnly) {
        StringBuilder b = new StringBuilder("[\n");
        Map<String, Integer> processIds = new HashMap<>();
        Map<String, String> openValue = new HashMap<>();
        int[] nextProcess = {0};
        long index = 0;
        long origin = history.isEmpty() ? 0 : ((Number) history.get(0).get("t")).longValue();
        for (Map<String, Object> op : history) {
            String f = String.valueOf(op.get("f"));
            if (!f.equals("deposit") && !f.equals("query")) {
                continue;
            }
            String name = String.valueOf(op.get("process"));
            if (finalReadsOnly && f.equals("query") && !name.equals("final-querier")) {
                continue;
            }
            String type = String.valueOf(op.get("type"));
            String key = String.valueOf(op.get("key"));
            int process = processIds.computeIfAbsent(name, n -> nextProcess[0]++);
            long time = (((Number) op.get("t")).longValue() - origin) * 1_000_000L;
            String value;
            if (f.equals("deposit")) {
                if (type.equals("invoke")) {
                    String dep = String.valueOf(op.get("value"));
                    value = "[[:append " + edn(key) + " " + dep.substring("dep-".length()) + "]]";
                    openValue.put(name, value);
                } else {
                    value = openValue.getOrDefault(name, "[]");
                }
            } else if (type.equals("ok") && op.get("value") instanceof List<?> answer) {
                StringBuilder list = new StringBuilder("[");
                for (Object d : answer) {
                    list.append(String.valueOf(d).substring("dep-".length())).append(' ');
                }
                value = "[[:r " + edn(key) + " " + list.toString().trim() + "]]]";
            } else {
                value = "[[:r " + edn(key) + " nil]]";
            }
            b.append("{:index ").append(index++).append(" :time ").append(time).append(" :type :").append(type)
                    .append(" :process ").append(process).append(" :f :txn :value ").append(value).append("}\n");
            if (type.equals("info")) {
                processIds.put(name, nextProcess[0]++); // Jepsen convention: a crashed process is replaced
            }
        }
        return b.append("]\n").toString();
    }

    private static String edn(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
