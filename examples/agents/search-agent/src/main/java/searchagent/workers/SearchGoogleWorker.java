package searchagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performs a Google search using the provided queries.
 * Returns search results with title, url, snippet, relevance, and source.
 */
public class SearchGoogleWorker implements Worker {

    private static final List<Map<String, Object>> GOOGLE_RESULTS_POOL = List.of(
            Map.of("title", "Quantum Computing Reaches New Milestone in Error Correction",
                    "url", "https://example.com/quantum-error-correction-2026",
                    "snippet", "Researchers have achieved a breakthrough in quantum error correction, reducing logical error rates by a factor of 100 compared to previous methods.",
                    "relevance", 0.95,
                    "source", "google"),
            Map.of("title", "The State of Quantum Computing: A 2026 Overview",
                    "url", "https://example.com/quantum-overview-2026",
                    "snippet", "Quantum computers now exceed 1000 logical qubits, enabling practical applications in drug discovery, materials science, and cryptography.",
                    "relevance", 0.93,
                    "source", "google"),
            Map.of("title", "Quantum Supremacy: From Theory to Practice",
                    "url", "https://example.com/quantum-supremacy-practical",
                    "snippet", "Multiple companies have demonstrated quantum advantage in optimization problems, marking a shift from laboratory experiments to commercial applications.",
                    "relevance", 0.89,
                    "source", "google"),
            Map.of("title", "How Quantum Computing is Transforming Cryptography",
                    "url", "https://example.com/quantum-cryptography-impact",
                    "snippet", "Post-quantum cryptographic standards are now being widely adopted as quantum computers pose increasing threats to classical encryption.",
                    "relevance", 0.85,
                    "source", "google"),
            Map.of("title", "Quantum Computing Market Analysis 2026",
                    "url", "https://example.com/quantum-market-2026",
                    "snippet", "The global quantum computing market has reached $12.4 billion, driven by enterprise adoption in financial services and pharmaceutical industries.",
                    "relevance", 0.82,
                    "source", "google")
    );

    @Override
    public String getTaskDefName() {
        return "sa_search_google";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<String> queries = (List<String>) task.getInputData().get("queries");
        if (queries == null) {
            queries = List.of();
        }

        Object maxResultsObj = task.getInputData().get("maxResults");
        int maxResults = 5;
        if (maxResultsObj instanceof Number) {
            maxResults = ((Number) maxResultsObj).intValue();
        }

        System.out.println("  [sa_search_google] Searching Google with " + queries.size()
                + " queries, maxResults=" + maxResults);

        int count = Math.min(maxResults, GOOGLE_RESULTS_POOL.size());
        List<Map<String, Object>> results = new ArrayList<>(GOOGLE_RESULTS_POOL.subList(0, count));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalScanned", 342);
        result.getOutputData().put("searchEngine", "google");
        return result;
    }
}
