package ragchromadb.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that demonstrates querying a ChromaDB collection.
 *
 * In production this would use:
 *   ChromaClient client = new ChromaClient("http://localhost:8000");
 *   Collection collection = client.getCollection("my_docs");
 *   QueryResponse results = collection.query(queryEmbeddings, nResults, where);
 */
public class ChromaQueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "chroma_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String collection = (String) task.getInputData().get("collection");
        Object nResultsObj = task.getInputData().get("nResults");
        int nResults = nResultsObj instanceof Number ? ((Number) nResultsObj).intValue() : 3;

        System.out.println("  [chromadb] Host: http://localhost:8000");
        System.out.println("  [chromadb] Collection: \"" + collection + "\", nResults=" + nResults);

        // deterministic ChromaDB query results
        Map<String, Object> results = Map.of(
                "ids", List.of(List.of("id-101", "id-204", "id-089")),
                "documents", List.of(List.of(
                        "ChromaDB is an open-source embedding database designed for AI applications.",
                        "Collections in ChromaDB store embeddings with optional metadata and documents.",
                        "ChromaDB supports persistent storage and runs locally or via Docker."
                )),
                "metadatas", List.of(List.of(
                        Map.of("source", "readme.md", "chunk", 1),
                        Map.of("source", "guide.md", "chunk", 4),
                        Map.of("source", "deploy.md", "chunk", 2)
                )),
                "distances", List.of(List.of(0.12, 0.19, 0.24))
        );

        Map<String, Object> collectionInfo = Map.of(
                "name", collection,
                "host", "localhost:8000",
                "count", 3492,
                "embeddingFunction", "default"
        );

        System.out.println("  [chromadb] Found 3 results from 3492 stored embeddings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("collectionInfo", collectionInfo);
        return result;
    }
}
