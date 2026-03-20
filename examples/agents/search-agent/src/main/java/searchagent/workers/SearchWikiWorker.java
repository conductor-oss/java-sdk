package searchagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performs a Wikipedia search using the provided queries.
 * Returns search results with title, url, snippet, relevance, and source.
 */
public class SearchWikiWorker implements Worker {

    private static final List<Map<String, Object>> WIKI_RESULTS_POOL = List.of(
            Map.of("title", "Quantum computing",
                    "url", "https://en.wikipedia.org/wiki/Quantum_computing",
                    "snippet", "A quantum computer is a computer that exploits quantum mechanical phenomena to perform calculations exponentially faster than classical computers for certain problem classes.",
                    "relevance", 0.91,
                    "source", "wikipedia"),
            Map.of("title", "Quantum error correction",
                    "url", "https://en.wikipedia.org/wiki/Quantum_error_correction",
                    "snippet", "Quantum error correction is used in quantum computing to protect quantum information from errors due to decoherence and other quantum noise.",
                    "relevance", 0.87,
                    "source", "wikipedia"),
            Map.of("title", "Quantum supremacy",
                    "url", "https://en.wikipedia.org/wiki/Quantum_supremacy",
                    "snippet", "Quantum supremacy is the goal of demonstrating that a programmable quantum computer can solve a problem that no classical computer can solve in any feasible amount of time.",
                    "relevance", 0.84,
                    "source", "wikipedia"),
            Map.of("title", "Qubit",
                    "url", "https://en.wikipedia.org/wiki/Qubit",
                    "snippet", "In quantum computing, a qubit is a quantum bit, the basic unit of quantum information and the quantum analogue of the classical binary bit.",
                    "relevance", 0.78,
                    "source", "wikipedia"),
            Map.of("title", "Shor's algorithm",
                    "url", "https://en.wikipedia.org/wiki/Shor%27s_algorithm",
                    "snippet", "Shor's algorithm is a quantum algorithm for finding the prime factors of an integer, demonstrating exponential speedup over the best known classical factoring algorithm.",
                    "relevance", 0.72,
                    "source", "wikipedia")
    );

    @Override
    public String getTaskDefName() {
        return "sa_search_wiki";
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

        System.out.println("  [sa_search_wiki] Searching Wikipedia with " + queries.size()
                + " queries, maxResults=" + maxResults);

        int count = Math.min(maxResults, WIKI_RESULTS_POOL.size());
        List<Map<String, Object>> results = new ArrayList<>(WIKI_RESULTS_POOL.subList(0, count));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalScanned", 47);
        result.getOutputData().put("searchEngine", "wikipedia");
        return result;
    }
}
