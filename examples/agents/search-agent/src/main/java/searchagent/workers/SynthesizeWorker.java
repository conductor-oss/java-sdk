package searchagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Synthesizes a final answer from ranked search results and top sources.
 * Produces a coherent answer string, confidence score, sources used, and answer type.
 */
public class SynthesizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sa_synthesize";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general query";
        }

        List<Map<String, Object>> rankedResults =
                (List<Map<String, Object>>) task.getInputData().get("rankedResults");
        List<String> topSources =
                (List<String>) task.getInputData().get("topSources");

        if (rankedResults == null) {
            rankedResults = List.of();
        }
        if (topSources == null) {
            topSources = List.of();
        }

        System.out.println("  [sa_synthesize] Synthesizing answer from "
                + rankedResults.size() + " results for: " + question);

        // Build answer from available snippets
        StringBuilder answerBuilder = new StringBuilder();
        answerBuilder.append("Based on analysis of ").append(rankedResults.size())
                .append(" sources regarding \"").append(question).append("\": ");

        for (int i = 0; i < Math.min(3, rankedResults.size()); i++) {
            Map<String, Object> item = rankedResults.get(i);
            Object snippet = item.get("snippet");
            if (snippet != null) {
                if (i > 0) {
                    answerBuilder.append(" Furthermore, ");
                }
                answerBuilder.append(snippet);
            }
        }

        if (rankedResults.isEmpty()) {
            answerBuilder.append("No relevant sources were found to answer this question.");
        }

        String answer = answerBuilder.toString();

        // Determine confidence based on number of results and source diversity
        double confidence = calculateConfidence(rankedResults);

        // Collect unique sources used
        List<String> sourcesUsed = rankedResults.stream()
                .map(r -> (String) r.get("source"))
                .filter(s -> s != null)
                .distinct()
                .toList();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("confidence", confidence);
        result.getOutputData().put("sourcesUsed", sourcesUsed);
        result.getOutputData().put("answerType", "synthesis");
        return result;
    }

    private double calculateConfidence(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return 0.1;
        }

        // Count distinct sources
        long sourceCount = results.stream()
                .map(r -> (String) r.get("source"))
                .filter(s -> s != null)
                .distinct()
                .count();

        // Base confidence on result count and source diversity
        double base = 0.6;
        double resultBonus = Math.min(0.2, results.size() * 0.04);
        double diversityBonus = sourceCount > 1 ? 0.12 : 0.0;

        double confidence = base + resultBonus + diversityBonus;
        // Cap at 0.98
        return Math.min(0.98, Math.round(confidence * 100.0) / 100.0);
    }
}
