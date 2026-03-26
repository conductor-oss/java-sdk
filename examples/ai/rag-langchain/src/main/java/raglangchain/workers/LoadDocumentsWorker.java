package raglangchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that loads documents from a source URL using WebBaseLoader.
 * Returns a list of documents with pageContent and metadata (source, page).
 */
public class LoadDocumentsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lc_load_documents";
    }

    @Override
    public TaskResult execute(Task task) {
        String sourceUrl = (String) task.getInputData().getOrDefault("sourceUrl",
                "https://example.com/docs");

        List<Map<String, Object>> documents = List.of(
                Map.of("pageContent", "LangChain is a framework for developing applications powered by language models.",
                        "metadata", Map.of("source", sourceUrl, "page", 1)),
                Map.of("pageContent", "Retrieval-Augmented Generation combines retrieval with generation to produce grounded answers.",
                        "metadata", Map.of("source", sourceUrl, "page", 2)),
                Map.of("pageContent", "Vector stores enable efficient similarity search over document embeddings.",
                        "metadata", Map.of("source", sourceUrl, "page", 3)),
                Map.of("pageContent", "Chains in LangChain allow composing multiple LLM calls and tools into workflows.",
                        "metadata", Map.of("source", sourceUrl, "page", 4))
        );

        System.out.println("  [load_documents] Loaded " + documents.size()
                + " documents from " + sourceUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        result.getOutputData().put("docCount", documents.size());
        result.getOutputData().put("loader", "WebBaseLoader");
        return result;
    }
}
