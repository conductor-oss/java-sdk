package datasync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Resolves conflicts using the specified strategy (e.g. latest_wins).
 * Input: changesInA, changesInB, conflicts, conflictStrategy
 * Output: toApplyA, toApplyB, resolved, resolvedCount
 */
public class ResolveConflictsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sy_resolve_conflicts";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> changesA =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("changesInA", List.of());
        List<Map<String, Object>> changesB =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("changesInB", List.of());
        List<Map<String, Object>> conflicts =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("conflicts", List.of());
        String strategy = (String) task.getInputData().getOrDefault("conflictStrategy", "latest_wins");

        List<Map<String, Object>> resolved = new ArrayList<>();
        for (Map<String, Object> c : conflicts) {
            String modA = (String) c.getOrDefault("modifiedA", "");
            String modB = (String) c.getOrDefault("modifiedB", "");
            String winner = modB.compareTo(modA) > 0 ? "B" : "A";
            String resolvedValue = "B".equals(winner)
                    ? (String) c.get("valueB") : (String) c.get("valueA");

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("recordId", c.get("recordId"));
            r.put("field", c.get("field"));
            r.put("resolvedValue", resolvedValue);
            r.put("resolvedBy", strategy + ": system " + winner);
            resolved.add(r);
        }

        // Non-conflicting changes from B -> apply to A
        List<Map<String, Object>> nonConflictB = new ArrayList<>();
        for (Map<String, Object> cb : changesB) {
            boolean isConflict = conflicts.stream().anyMatch(cf ->
                    cf.get("recordId").equals(cb.get("recordId"))
                            && cf.get("field").equals(cb.get("field")));
            if (!isConflict) nonConflictB.add(cb);
        }

        // Non-conflicting changes from A -> apply to B
        List<Map<String, Object>> nonConflictA = new ArrayList<>();
        for (Map<String, Object> ca : changesA) {
            boolean isConflict = conflicts.stream().anyMatch(cf ->
                    cf.get("recordId").equals(ca.get("recordId"))
                            && cf.get("field").equals(ca.get("field")));
            if (!isConflict) nonConflictA.add(ca);
        }

        // Add resolved conflicts to appropriate system
        List<Map<String, Object>> toApplyA = new ArrayList<>(nonConflictB);
        List<Map<String, Object>> toApplyB = new ArrayList<>(nonConflictA);
        for (Map<String, Object> r : resolved) {
            String resolvedBy = (String) r.get("resolvedBy");
            Map<String, Object> update = Map.of(
                    "recordId", r.get("recordId"),
                    "field", r.get("field"),
                    "newValue", r.get("resolvedValue"));
            if (resolvedBy.contains("system B")) {
                toApplyA.add(update);
            } else {
                toApplyB.add(update);
            }
        }

        System.out.println("  [resolve] Resolved " + resolved.size() + " conflicts using \""
                + strategy + "\", to apply: " + toApplyA.size() + " -> A, " + toApplyB.size() + " -> B");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toApplyA", toApplyA);
        result.getOutputData().put("toApplyB", toApplyB);
        result.getOutputData().put("resolved", resolved);
        result.getOutputData().put("resolvedCount", resolved.size());
        return result;
    }
}
