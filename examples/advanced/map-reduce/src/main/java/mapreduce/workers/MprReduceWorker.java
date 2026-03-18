package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reduce worker: aggregates mapped results from all 3 map workers.
 * Sums up all counts and produces a combined result list.
 *
 * Input: mapped1, mapped2, mapped3 (each a list of {docIndex, count})
 * Output: reduced (combined list), totalOccurrences (int)
 */
public class MprReduceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mpr_reduce";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> all = new ArrayList<>();

        // Collect results from all three mappers
        for (String key : List.of("mapped1", "mapped2", "mapped3")) {
            Object mapObj = task.getInputData().get(key);
            if (mapObj instanceof List) {
                for (Object item : (List<?>) mapObj) {
                    if (item instanceof Map) {
                        all.add((Map<String, Object>) item);
                    }
                }
            }
        }

        // Sum total occurrences
        int totalOccurrences = 0;
        for (Map<String, Object> entry : all) {
            Object countObj = entry.get("count");
            if (countObj instanceof Number) {
                totalOccurrences += ((Number) countObj).intValue();
            }
        }

        // Filter to only docs with count > 0
        List<Map<String, Object>> nonZero = all.stream()
                .filter(e -> {
                    Object c = e.get("count");
                    return c instanceof Number && ((Number) c).intValue() > 0;
                })
                .toList();

        System.out.println("  [reduce] Aggregated " + all.size() + " entries, totalOccurrences="
                + totalOccurrences + ", non-zero docs=" + nonZero.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reduced", nonZero);
        result.getOutputData().put("totalOccurrences", totalOccurrences);
        result.getOutputData().put("totalDocuments", all.size());
        return result;
    }
}
