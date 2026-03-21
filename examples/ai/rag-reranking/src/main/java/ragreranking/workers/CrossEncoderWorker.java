package ragreranking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that re-ranks candidates using a cross-encoder model.
 * Performs cross-encoder scoring where order changes significantly
 * compared to bi-encoder scores.
 *
 * In production this would call Cohere Rerank API or run
 * cross-encoder/ms-marco-MiniLM-L-6-v2 locally.
 */
public class CrossEncoderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rerank_crossencoder";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object topNObj = task.getInputData().get("topN");
        int topN = 3;
        if (topNObj instanceof Number) {
            topN = ((Number) topNObj).intValue();
        } else if (topNObj instanceof String) {
            try {
                topN = Integer.parseInt((String) topNObj);
            } catch (NumberFormatException ignored) {
            }
        }

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) task.getInputData().get("candidates");

        if (candidates == null) {
            candidates = List.of();
        }

        System.out.println("  [rerank] Scoring " + candidates.size()
                + " candidates with cross-encoder model");
        System.out.println("  [rerank] Model: cross-encoder/ms-marco-MiniLM-L-6-v2");

        // Perform cross-encoder re-scoring (different order than bi-encoder)
        // Fixed cross-encoder scores assigned by candidate index position
        double[] crossScores = {0.94, 0.97, 0.72, 0.31, 0.45, 0.91};

        List<Map<String, Object>> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> candidate = candidates.get(i);
            Map<String, Object> entry = new HashMap<>(candidate);
            if (i < crossScores.length) {
                entry.put("crossEncoderScore", crossScores[i]);
            } else {
                entry.put("crossEncoderScore", 0.0);
            }
            scored.add(entry);
        }

        // Sort by crossEncoderScore descending
        scored.sort((a, b) -> {
            double sa = ((Number) a.get("crossEncoderScore")).doubleValue();
            double sb = ((Number) b.get("crossEncoderScore")).doubleValue();
            return Double.compare(sb, sa);
        });

        List<Map<String, Object>> reranked = scored.subList(0, Math.min(topN, scored.size()));

        System.out.println("  [rerank] Top-" + topN + " after re-ranking:");
        for (Map<String, Object> r : reranked) {
            String text = (String) r.get("text");
            String preview = text.length() > 50 ? text.substring(0, 50) + "..." : text;
            System.out.println("    - " + r.get("id")
                    + ": bi-encoder=" + r.get("biEncoderScore")
                    + " -> cross-encoder=" + r.get("crossEncoderScore")
                    + " \"" + preview + "\"");
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reranked", reranked);
        result.getOutputData().put("rerankedCount", reranked.size());
        result.getOutputData().put("model", "cross-encoder/ms-marco-MiniLM-L-6-v2");
        return result;
    }
}
