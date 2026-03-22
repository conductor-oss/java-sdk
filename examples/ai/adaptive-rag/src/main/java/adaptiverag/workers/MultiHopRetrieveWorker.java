package adaptiverag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-hop retrieval worker that gathers documents across two hops
 * for analytical queries. Uses Jaccard similarity (word overlap) to find
 * relevant documents, then expands with related concepts in a second hop.
 *
 * <p>For production use, replace this with a real vector database search.
 */
public class MultiHopRetrieveWorker implements Worker {

    /** Hop-1 documents: direct factual content. */
    private static final List<Map<String, Object>> HOP1_DOCS = List.of(
            Map.of("text", "Conductor uses a centralized orchestrator for task coordination. "
                    + "The server manages workflow state and distributes tasks to workers.", "hop", 1),
            Map.of("text", "Temporal uses a decentralized model where workflows run as code. "
                    + "Workflow logic is expressed as regular functions in the host language.", "hop", 1),
            Map.of("text", "Conductor provides a visual workflow designer and real-time monitoring dashboard.", "hop", 1),
            Map.of("text", "Temporal SDKs support Java, Go, Python, TypeScript, and .NET for workflow development.", "hop", 1)
    );

    /** Hop-2 documents: deeper analysis and comparisons. */
    private static final List<Map<String, Object>> HOP2_DOCS = List.of(
            Map.of("text", "Orchestration provides visibility but adds a central dependency. "
                    + "The orchestrator is a single point of coordination.", "hop", 2),
            Map.of("text", "Choreography reduces coupling but complicates debugging. "
                    + "Without a central coordinator, tracing failures requires distributed tracing.", "hop", 2),
            Map.of("text", "Conductor workflows are defined as JSON, enabling versioning and tooling integration.", "hop", 2),
            Map.of("text", "Temporal's replay-based architecture enables long-running workflows with automatic state recovery.", "hop", 2)
    );

    @Override
    public String getTaskDefName() {
        return "ar_mhop_ret";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'question' is required and must not be blank");
            return fail;
        }

        System.out.println("  [multi-hop] Deep retrieval for: \"" + question + "\"");

        Set<String> queryTokens = tokenize(question);

        // Hop 1: find the 2 most relevant direct documents
        List<Map<String, Object>> hop1Results = rankByRelevance(HOP1_DOCS, queryTokens, 2);

        // Expand query tokens with terms from hop-1 results for the second hop
        Set<String> expandedTokens = new HashSet<>(queryTokens);
        for (Map<String, Object> doc : hop1Results) {
            expandedTokens.addAll(tokenize((String) doc.get("text")));
        }

        // Hop 2: find the 2 most relevant analysis documents using expanded query
        List<Map<String, Object>> hop2Results = rankByRelevance(HOP2_DOCS, expandedTokens, 2);

        List<Map<String, Object>> documents = new ArrayList<>();
        documents.addAll(hop1Results);
        documents.addAll(hop2Results);

        System.out.println("  [multi-hop] " + documents.size() + " docs across 2 hops");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        result.getOutputData().put("hops", 2);
        return result;
    }

    private List<Map<String, Object>> rankByRelevance(List<Map<String, Object>> docs,
                                                       Set<String> queryTokens, int limit) {
        List<Map.Entry<Map<String, Object>, Double>> scored = new ArrayList<>();
        for (Map<String, Object> doc : docs) {
            Set<String> docTokens = tokenize((String) doc.get("text"));
            double similarity = jaccardSimilarity(queryTokens, docTokens);
            scored.add(Map.entry(doc, similarity));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return scored.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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
