package ragknowledgegraph.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Worker that merges context from graph traversal and vector search.
 * Combines graphFacts, graphRelations, and vectorDocs into a unified
 * context with graphSummary, vectorSummary, and totalSources count.
 */
public class MergeContextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kg_merge_context";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> graphFacts =
                (List<Map<String, Object>>) task.getInputData().get("graphFacts");
        if (graphFacts == null) {
            graphFacts = List.of();
        }

        List<Map<String, Object>> graphRelations =
                (List<Map<String, Object>>) task.getInputData().get("graphRelations");
        if (graphRelations == null) {
            graphRelations = List.of();
        }

        List<Map<String, Object>> vectorDocs =
                (List<Map<String, Object>>) task.getInputData().get("vectorDocs");
        if (vectorDocs == null) {
            vectorDocs = List.of();
        }

        System.out.println("  [merge_context] Merging " + graphFacts.size() + " facts, "
                + graphRelations.size() + " relations, " + vectorDocs.size() + " vector docs");

        // Build graph summary from facts
        String graphSummary = graphFacts.stream()
                .map(f -> f.get("subject") + " " + f.get("predicate") + " " + f.get("object"))
                .collect(Collectors.joining("; "));

        // Build vector summary from documents
        String vectorSummary = vectorDocs.stream()
                .map(d -> (String) d.get("text"))
                .collect(Collectors.joining(" | "));

        int totalSources = graphFacts.size() + vectorDocs.size();

        Map<String, Object> mergedContext = new HashMap<>();
        mergedContext.put("graphSummary", graphSummary);
        mergedContext.put("vectorSummary", vectorSummary);
        mergedContext.put("totalSources", totalSources);

        System.out.println("  [merge_context] Merged context with " + totalSources + " total sources");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedContext", mergedContext);
        return result;
    }
}
