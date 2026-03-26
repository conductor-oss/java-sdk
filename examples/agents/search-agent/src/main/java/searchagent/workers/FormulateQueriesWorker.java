package searchagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Takes a user question and formulates optimized search queries.
 * Returns a list of queries, the detected intent, and complexity level.
 */
public class FormulateQueriesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_formulate_queries";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general information";
        }

        Object maxResultsObj = task.getInputData().get("maxResults");
        int maxResults = 5;
        if (maxResultsObj instanceof Number) {
            maxResults = ((Number) maxResultsObj).intValue();
        }

        System.out.println("  [sa_formulate_queries] Formulating queries for: " + question);

        // Derive queries from the question by extracting key terms
        String normalized = question.toLowerCase().replaceAll("[?.,!]", "").trim();
        String[] words = normalized.split("\\s+");

        // Build three query variants from the question
        String baseQuery = String.join(" ", words);
        String focusedQuery = buildFocusedQuery(words);
        String advancedQuery = buildAdvancedQuery(words);

        List<String> queries = List.of(baseQuery, focusedQuery, advancedQuery);

        // Determine intent based on question structure
        String intent = determineIntent(normalized);

        // Determine complexity based on word count and structure
        String complexity = determineComplexity(words, normalized);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queries", queries);
        result.getOutputData().put("intent", intent);
        result.getOutputData().put("complexity", complexity);
        result.getOutputData().put("originalQuestion", question);
        result.getOutputData().put("maxResults", maxResults);
        return result;
    }

    private String buildFocusedQuery(String[] words) {
        // Take content words (skip short stop words) for a focused query
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 3) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }
        String focused = sb.toString().trim();
        if (focused.isEmpty()) {
            focused = String.join(" ", words);
        }
        return focused + " practical applications";
    }

    private String buildAdvancedQuery(String[] words) {
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 4) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }
        String advanced = sb.toString().trim();
        if (advanced.isEmpty()) {
            advanced = String.join(" ", words);
        }
        return advanced + " latest advances";
    }

    private String determineIntent(String question) {
        if (question.startsWith("how")) {
            return "how_to";
        } else if (question.startsWith("why")) {
            return "explanation";
        } else if (question.startsWith("compare") || question.contains("vs")
                || question.contains("versus") || question.contains("difference")) {
            return "comparison";
        } else if (question.contains("best") || question.contains("recommend")
                || question.contains("top")) {
            return "recommendation";
        }
        return "factual_research";
    }

    private String determineComplexity(String[] words, String question) {
        if (words.length > 10 || question.contains("current state")
                || question.contains("comprehensive") || question.contains("analysis")) {
            return "high";
        } else if (words.length > 5) {
            return "medium";
        }
        return "low";
    }
}
