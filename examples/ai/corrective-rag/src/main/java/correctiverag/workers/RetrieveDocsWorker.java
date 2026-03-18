package correctiverag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieves documents from a bundled knowledge base using Jaccard similarity
 * (word overlap). Returns the top 3 most relevant documents with their
 * computed relevance scores.
 *
 * <p>The bundled knowledge base covers technical Conductor topics, so queries
 * about pricing, billing, or other off-topic subjects will naturally produce
 * low relevance scores, triggering the corrective path (web search fallback).
 */
public class RetrieveDocsWorker implements Worker {

    /** Bundled technical documents about Conductor features. */
    private static final List<Map<String, String>> KNOWLEDGE_BASE = List.of(
            Map.of("id", "d1",
                    "text", "Conductor supports dynamic fork for parallel task execution."),
            Map.of("id", "d2",
                    "text", "Event handlers can trigger workflows from external events."),
            Map.of("id", "d3",
                    "text", "Task domains enable environment-based routing."),
            Map.of("id", "d4",
                    "text", "Conductor workflows are defined as JSON and support versioning."),
            Map.of("id", "d5",
                    "text", "Workers poll the Conductor server for tasks and report results back."),
            Map.of("id", "d6",
                    "text", "Sub-workflows allow composing complex pipelines from reusable building blocks.")
    );

    @Override
    public String getTaskDefName() {
        return "cr_retrieve_docs";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "";
        }

        System.out.println("  [retrieve] Searching vector store for: \"" + question + "\"");

        Set<String> queryTokens = tokenize(question);

        List<Map<String, Object>> scoredDocs = new ArrayList<>();
        for (Map<String, String> doc : KNOWLEDGE_BASE) {
            Set<String> docTokens = tokenize(doc.get("text"));
            double relevance = jaccardSimilarity(queryTokens, docTokens);
            relevance = Math.round(relevance * 100.0) / 100.0;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", doc.get("id"));
            entry.put("text", doc.get("text"));
            entry.put("relevance", relevance);
            scoredDocs.add(entry);
        }

        // Sort by relevance descending, take top 3
        scoredDocs.sort((a, b) -> Double.compare(
                ((Number) b.get("relevance")).doubleValue(),
                ((Number) a.get("relevance")).doubleValue()));
        List<Map<String, Object>> documents = scoredDocs.stream()
                .limit(3)
                .collect(Collectors.toList());

        double avgRelevance = documents.stream()
                .mapToDouble(d -> ((Number) d.get("relevance")).doubleValue())
                .average().orElse(0.0);

        String quality = avgRelevance < 0.3 ? "low relevance" : "moderate/high relevance";
        System.out.println("  [retrieve] Found " + documents.size() + " docs (" + quality
                + ", avg=" + String.format("%.2f", avgRelevance) + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(t -> !t.isBlank() && t.length() > 1)
                .collect(Collectors.toSet());
    }

    private static double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
}
