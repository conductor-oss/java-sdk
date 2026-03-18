package adaptiverag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple retrieval worker that finds relevant documents using Jaccard similarity
 * (word overlap) against a bundled knowledge base.
 *
 * <p>For production use, replace this with a real vector database search.
 */
public class SimpleRetrieveWorker implements Worker {

    /** Bundled knowledge base documents for word-overlap retrieval. */
    private static final List<String> KNOWLEDGE_BASE = List.of(
            "Conductor is an open-source workflow orchestration platform originally built at Netflix.",
            "Orkes provides a managed cloud version of Conductor with enterprise features.",
            "Workflow orchestration coordinates multiple services to complete a business process.",
            "Conductor supports task workers written in Java, Python, Go, C#, and JavaScript.",
            "Task definitions specify the contract for worker inputs and outputs.",
            "Conductor workflows are defined as JSON and can be versioned.",
            "The Conductor UI provides real-time visibility into workflow executions.",
            "Workers poll the Conductor server for tasks, execute them, and report results."
    );

    @Override
    public String getTaskDefName() {
        return "ar_simple_ret";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "";
        }

        System.out.println("  [simple-ret] Quick lookup: \"" + question + "\"");

        // Compute Jaccard similarity between the question and each document
        Set<String> queryTokens = tokenize(question);

        List<Map.Entry<String, Double>> scored = new ArrayList<>();
        for (String doc : KNOWLEDGE_BASE) {
            Set<String> docTokens = tokenize(doc);
            double similarity = jaccardSimilarity(queryTokens, docTokens);
            scored.add(Map.entry(doc, similarity));
        }

        // Sort by similarity descending, take top 2
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> documents = scored.stream()
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("  [simple-ret] Returning " + documents.size() + " most relevant documents");

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
