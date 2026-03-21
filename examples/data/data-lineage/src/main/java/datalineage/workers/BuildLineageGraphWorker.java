package datalineage.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a lineage graph summary from collected lineage entries.
 * Input: lineage (list), recordCount (int)
 * Output: transformSteps (int), depth (int), summary (string), lineageChain (string)
 */
public class BuildLineageGraphWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ln_build_lineage_graph";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> lineage = (List<Map<String, Object>>) task.getInputData().get("lineage");
        if (lineage == null) {
            lineage = List.of();
        }
        Object recordCountObj = task.getInputData().get("recordCount");
        int recordCount = 0;
        if (recordCountObj instanceof Number) {
            recordCount = ((Number) recordCountObj).intValue();
        }

        String chain = lineage.stream()
                .map(l -> l.get("name") != null ? l.get("name").toString() : "unknown")
                .collect(Collectors.joining(" -> "));
        String summary = "Lineage: " + chain + " (" + lineage.size() + " steps, " + recordCount + " records)";

        System.out.println("  [graph] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformSteps", lineage.size());
        result.getOutputData().put("depth", lineage.size());
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("lineageChain", chain);
        return result;
    }
}
