package semistructuredrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that merges structured and unstructured search results into a
 * unified context string. Formats structured results as [TABLE] entries
 * and unstructured results as [TEXT] entries.
 */
public class MergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ss_merge_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> structuredResults =
                (List<Map<String, Object>>) task.getInputData().get("structuredResults");
        if (structuredResults == null) {
            structuredResults = List.of();
        }

        List<Map<String, Object>> unstructuredResults =
                (List<Map<String, Object>>) task.getInputData().get("unstructuredResults");
        if (unstructuredResults == null) {
            unstructuredResults = List.of();
        }

        System.out.println("  [merge] Merging " + structuredResults.size()
                + " structured + " + unstructuredResults.size() + " unstructured results");

        StringBuilder sb = new StringBuilder();

        for (Map<String, Object> sr : structuredResults) {
            sb.append("[TABLE] ").append(sr.get("field"))
                    .append(" = ").append(sr.get("value"))
                    .append(" (from ").append(sr.get("table")).append(")\n");
        }

        for (Map<String, Object> ur : unstructuredResults) {
            sb.append("[TEXT] ").append(ur.get("snippet"))
                    .append(" (chunk ").append(ur.get("chunkId"))
                    .append(", score ").append(ur.get("score")).append(")\n");
        }

        String mergedContext = sb.toString().trim();
        int totalDocs = structuredResults.size() + unstructuredResults.size();

        System.out.println("  [merge] Merged context with " + totalDocs + " total documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedContext", mergedContext);
        result.getOutputData().put("totalDocs", totalDocs);
        return result;
    }
}
