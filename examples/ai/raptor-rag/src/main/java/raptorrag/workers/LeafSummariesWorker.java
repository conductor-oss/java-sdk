package raptorrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that creates leaf-level summaries from document chunks.
 * Groups related chunks and produces 4 leaf summaries at level 0
 * of the RAPTOR tree hierarchy.
 */
public class LeafSummariesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rp_leaf_summaries";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) task.getInputData().get("chunks");
        int chunkCount = (chunks != null) ? chunks.size() : 0;

        System.out.println("  [leaf_summaries] Summarizing " + chunkCount + " chunks into leaf nodes");

        // Fixed deterministic leaf summaries at level 0
        List<Map<String, Object>> leafSummaries = List.of(
                Map.of("id", "leaf-1", "text",
                        "Conductor is an orchestration platform for distributed apps with DAG-based workflow definitions.",
                        "chunkIds", List.of("chunk-1", "chunk-2"),
                        "level", 0),
                Map.of("id", "leaf-2", "text",
                        "Task workers are polyglot, supporting Java, Python, Go, and other languages via HTTP.",
                        "chunkIds", List.of("chunk-3"),
                        "level", 0),
                Map.of("id", "leaf-3", "text",
                        "Conductor includes retries, rate limiting, prioritization, and workflow versioning for reliability.",
                        "chunkIds", List.of("chunk-4", "chunk-5"),
                        "level", 0),
                Map.of("id", "leaf-4", "text",
                        "The event system supports reactive workflows triggered by external events and signals.",
                        "chunkIds", List.of("chunk-6"),
                        "level", 0)
        );

        System.out.println("  [leaf_summaries] Created " + leafSummaries.size() + " leaf summaries (level 0)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("leafSummaries", leafSummaries);
        result.getOutputData().put("leafCount", leafSummaries.size());
        return result;
    }
}
