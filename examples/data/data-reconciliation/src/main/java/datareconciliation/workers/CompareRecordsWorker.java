package datareconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares records from two sources and identifies matches, mismatches, and missing records.
 * Input: recordsA, recordsB, keyField
 * Output: matched, mismatched, missingInA, missingInB, matchedCount, mismatchedCount
 */
public class CompareRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rc_compare_records";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> a = (List<Map<String, Object>>) task.getInputData().get("recordsA");
        List<Map<String, Object>> b = (List<Map<String, Object>>) task.getInputData().get("recordsB");
        String key = (String) task.getInputData().getOrDefault("keyField", "orderId");

        if (a == null) a = List.of();
        if (b == null) b = List.of();

        Map<String, Map<String, Object>> mapA = new LinkedHashMap<>();
        for (Map<String, Object> r : a) {
            mapA.put(String.valueOf(r.get(key)), r);
        }
        Map<String, Map<String, Object>> mapB = new LinkedHashMap<>();
        for (Map<String, Object> r : b) {
            mapB.put(String.valueOf(r.get(key)), r);
        }

        List<String> matched = new ArrayList<>();
        List<Map<String, Object>> mismatched = new ArrayList<>();
        List<String> missingInB = new ArrayList<>();
        List<String> missingInA = new ArrayList<>();

        for (String k : mapA.keySet()) {
            if (mapB.containsKey(k)) {
                List<String> diffs = new ArrayList<>();
                for (String field : mapA.get(k).keySet()) {
                    String valA = String.valueOf(mapA.get(k).get(field));
                    String valB = String.valueOf(mapB.get(k).get(field));
                    if (!valA.equals(valB)) {
                        diffs.add(field);
                    }
                }
                if (diffs.isEmpty()) {
                    matched.add(k);
                } else {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", k);
                    m.put("differingFields", diffs);
                    m.put("sourceA", mapA.get(k));
                    m.put("sourceB", mapB.get(k));
                    mismatched.add(m);
                }
            } else {
                missingInB.add(k);
            }
        }

        for (String k : mapB.keySet()) {
            if (!mapA.containsKey(k)) {
                missingInA.add(k);
            }
        }

        System.out.println("  [compare] Matched: " + matched.size() + ", Mismatched: " + mismatched.size()
                + ", Missing in A: " + missingInA.size() + ", Missing in B: " + missingInB.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("matched", matched);
        result.getOutputData().put("mismatched", mismatched);
        result.getOutputData().put("missingInA", missingInA);
        result.getOutputData().put("missingInB", missingInB);
        result.getOutputData().put("matchedCount", matched.size());
        result.getOutputData().put("mismatchedCount", mismatched.size());
        return result;
    }
}
