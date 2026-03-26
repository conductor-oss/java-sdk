package semistructuredrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches unstructured text chunks for relevant passages.
 * Returns results with chunkId, relevance score, and snippet.
 */
public class SearchUnstructuredWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ss_search_unstructured";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, String>> textChunks =
                (List<Map<String, String>>) task.getInputData().get("textChunks");
        int chunkCount = (textChunks != null) ? textChunks.size() : 0;

        System.out.println("  [search-unstructured] Searching " + chunkCount + " text chunks");

        List<Map<String, Object>> results = List.of(
                Map.of("chunkId", "chunk-001", "score", 0.91,
                        "snippet", "Q3 earnings exceeded expectations with 15% growth."),
                Map.of("chunkId", "chunk-003", "score", 0.85,
                        "snippet", "Market trends indicate strong demand in cloud services.")
        );

        System.out.println("  [search-unstructured] Found " + results.size() + " unstructured results");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
