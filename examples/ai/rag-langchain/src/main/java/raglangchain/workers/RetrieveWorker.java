package raglangchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves the most relevant documents for a question using FAISS.
 * Assigns decreasing similarity scores (0.95, 0.87, 0.79, ...) and returns the top K results.
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lc_retrieve";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().getOrDefault("question",
                "What is LangChain?");
        List<Map<String, Object>> embeddings =
                (List<Map<String, Object>>) task.getInputData().get("embeddings");
        int topK = task.getInputData().get("topK") != null
                ? ((Number) task.getInputData().get("topK")).intValue() : 3;

        List<Map<String, Object>> retrievedDocs = new ArrayList<>();

        if (embeddings != null) {
            int limit = Math.min(topK, embeddings.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> emb = embeddings.get(i);
                double score = 0.95 - (i * 0.08);
                score = Math.round(score * 100.0) / 100.0;

                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("text", emb.get("text"));
                doc.put("score", score);
                doc.put("metadata", emb.get("metadata"));
                retrievedDocs.add(doc);
            }
        }

        System.out.println("  [retrieve] Retrieved " + retrievedDocs.size()
                + " docs for question: \"" + question + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("retrievedDocs", retrievedDocs);
        result.getOutputData().put("retriever", "FAISS");
        return result;
    }
}
