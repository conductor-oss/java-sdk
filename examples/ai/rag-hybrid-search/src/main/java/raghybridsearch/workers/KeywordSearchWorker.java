package raghybridsearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keyword (BM25) search worker.
 * Tokenizes the query and computes real BM25 scores against bundled documents.
 */
public class KeywordSearchWorker implements Worker {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final List<Map<String, String>> DOCUMENTS = List.of(
            Map.of("id", "doc-1", "text", "Conductor is an open-source workflow orchestration platform for microservices."),
            Map.of("id", "doc-2", "text", "Workers poll tasks from the Conductor server and execute business logic independently."),
            Map.of("id", "doc-3", "text", "Conductor uses a JSON DSL to define workflows declaratively with task mappings."),
            Map.of("id", "doc-4", "text", "The Conductor server is the central orchestration engine managing workflow state."),
            Map.of("id", "doc-5", "text", "Conductor supports retry policies, timeouts, rate limiting, and circuit breakers."),
            Map.of("id", "doc-6", "text", "Workflow definitions include task mappings, input templates, and output parameters."),
            Map.of("id", "doc-7", "text", "Conductor provides a REST API for workflow management and task execution."),
            Map.of("id", "doc-8", "text", "The Java SDK for Conductor simplifies worker implementation and workflow registration.")
    );

    @Override
    public String getTaskDefName() {
        return "hs_keyword_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) question = "";

        System.out.println("  [keyword] BM25 scoring for: \"" + question + "\"");

        List<String> queryTerms = tokenize(question);
        double avgDl = DOCUMENTS.stream().mapToInt(d -> tokenize(d.get("text")).size()).average().orElse(1);
        int N = DOCUMENTS.size();

        // Compute IDF for each query term
        Map<String, Double> idf = new LinkedHashMap<>();
        for (String term : queryTerms) {
            long df = DOCUMENTS.stream().filter(d -> tokenize(d.get("text")).contains(term)).count();
            idf.put(term, Math.log((N - df + 0.5) / (df + 0.5) + 1));
        }

        // Score each document
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, String> doc : DOCUMENTS) {
            List<String> docTerms = tokenize(doc.get("text"));
            int dl = docTerms.size();
            double score = 0;
            for (String term : queryTerms) {
                long tf = docTerms.stream().filter(t -> t.equals(term)).count();
                double termScore = idf.getOrDefault(term, 0.0)
                        * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * dl / avgDl));
                score += termScore;
            }
            if (score > 0) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", doc.get("id"));
                entry.put("score", Math.round(score * 100.0) / 100.0);
                entry.put("text", doc.get("text"));
                scored.add(entry);
            }
        }

        scored.sort(Comparator.comparingDouble((Map<String, Object> m) -> (double) m.get("score")).reversed());
        List<Map<String, Object>> topK = scored.subList(0, Math.min(3, scored.size()));

        System.out.println("  [keyword] Found " + topK.size() + " results (BM25)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", topK);
        result.getOutputData().put("count", topK.size());
        return result;
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toList());
    }
}
