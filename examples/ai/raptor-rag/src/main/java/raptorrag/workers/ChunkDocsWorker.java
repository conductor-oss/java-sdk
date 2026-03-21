package raptorrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that chunks a document into smaller segments for RAPTOR tree construction.
 * Takes documentText and returns 6 fixed chunks, each with id, text, and token count.
 */
public class ChunkDocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rp_chunk_docs";
    }

    @Override
    public TaskResult execute(Task task) {
        String documentText = (String) task.getInputData().get("documentText");
        if (documentText == null) {
            documentText = "";
        }

        System.out.println("  [chunk_docs] Chunking document (" + documentText.length() + " chars)");

        // Fixed deterministic chunks
        List<Map<String, Object>> chunks = List.of(
                Map.of("id", "chunk-1", "text",
                        "Conductor is an open-source orchestration platform for building distributed applications.",
                        "tokens", 14),
                Map.of("id", "chunk-2", "text",
                        "Workflows are defined as DAGs of tasks with support for branching and looping.",
                        "tokens", 15),
                Map.of("id", "chunk-3", "text",
                        "Task workers can be implemented in any language including Java, Python, and Go.",
                        "tokens", 15),
                Map.of("id", "chunk-4", "text",
                        "Conductor provides built-in support for retries, rate limiting, and task prioritization.",
                        "tokens", 13),
                Map.of("id", "chunk-5", "text",
                        "Workflow versioning allows multiple versions to run concurrently during migration.",
                        "tokens", 12),
                Map.of("id", "chunk-6", "text",
                        "The event system enables reactive workflows triggered by external events and signals.",
                        "tokens", 13)
        );

        System.out.println("  [chunk_docs] Created " + chunks.size() + " chunks");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chunks", chunks);
        result.getOutputData().put("chunkCount", chunks.size());
        return result;
    }
}
