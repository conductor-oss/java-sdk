package raghybridsearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vector similarity search worker.
 * Computes TF-IDF vectors and uses cosine similarity to rank bundled documents.
 */
public class VectorSearchWorker implements Worker {

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
        return "hs_vector_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) question = "";

        System.out.println("  [vector] Computing TF-IDF cosine similarity for: \"" + question + "\"");

        // Build vocabulary
        Set<String> vocab = new java.util.LinkedHashSet<>();
        List<List<String>> docTokens = new ArrayList<>();
        for (Map<String, String> doc : DOCUMENTS) {
            List<String> tokens = tokenize(doc.get("text"));
            docTokens.add(tokens);
            vocab.addAll(tokens);
        }
        List<String> queryTokens = tokenize(question);
        vocab.addAll(queryTokens);

        List<String> vocabList = new ArrayList<>(vocab);
        int N = DOCUMENTS.size();

        // Compute IDF
        Map<String, Double> idf = new HashMap<>();
        for (String term : vocabList) {
            long df = docTokens.stream().filter(s -> s.contains(term)).count();
            idf.put(term, Math.log((double) (N + 1) / (df + 1)) + 1);
        }

        // Query TF-IDF vector
        double[] queryVec = tfidfVector(queryTokens, vocabList, idf);

        // Score each document by cosine similarity
        List<Map<String, Object>> scored = new ArrayList<>();
        for (int i = 0; i < DOCUMENTS.size(); i++) {
            double[] docVec = tfidfVector(docTokens.get(i), vocabList, idf);
            double sim = cosineSimilarity(queryVec, docVec);
            if (sim > 0.01) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", DOCUMENTS.get(i).get("id"));
                entry.put("score", Math.round(sim * 100.0) / 100.0);
                entry.put("text", DOCUMENTS.get(i).get("text"));
                scored.add(entry);
            }
        }

        scored.sort(Comparator.comparingDouble((Map<String, Object> m) -> (double) m.get("score")).reversed());
        List<Map<String, Object>> topK = scored.subList(0, Math.min(3, scored.size()));

        System.out.println("  [vector] Found " + topK.size() + " results (cosine similarity)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", topK);
        result.getOutputData().put("count", topK.size());
        return result;
    }

    private double[] tfidfVector(List<String> tokens, List<String> vocab, Map<String, Double> idf) {
        double[] vec = new double[vocab.size()];
        for (int i = 0; i < vocab.size(); i++) {
            String term = vocab.get(i);
            long tf = tokens.stream().filter(t -> t.equals(term)).count();
            vec[i] = tf * idf.getOrDefault(term, 0.0);
        }
        return vec;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toList());
    }
}
