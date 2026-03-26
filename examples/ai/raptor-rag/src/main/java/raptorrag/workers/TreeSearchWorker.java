package raptorrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches the RAPTOR tree for context relevant to a question.
 * Traverses the tree top-down (root -> clusters -> leaves) and returns
 * context at 3 levels with relevance scores.
 */
public class TreeSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rp_tree_search";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, Object>> leafSummaries =
                (List<Map<String, Object>>) task.getInputData().get("leafSummaries");
        List<Map<String, Object>> clusterSummaries =
                (List<Map<String, Object>>) task.getInputData().get("clusterSummaries");
        Map<String, Object> treeSummary =
                (Map<String, Object>) task.getInputData().get("treeSummary");

        String preview = question.length() > 60
                ? question.substring(0, 60) + "..."
                : question;
        System.out.println("  [tree_search] Searching tree for: \"" + preview + "\"");

        // Fixed deterministic multi-level context
        Map<String, Object> rootLevel = Map.of(
                "level", 2,
                "source", "root",
                "text", treeSummary != null ? treeSummary.get("text") : "Conductor orchestration platform overview.",
                "relevance", 0.75
        );

        Map<String, Object> clusterLevel = Map.of(
                "level", 1,
                "source", "cluster-1",
                "text", clusterSummaries != null && !clusterSummaries.isEmpty()
                        ? clusterSummaries.get(0).get("text")
                        : "Conductor provides DAG-based orchestration with polyglot worker support.",
                "relevance", 0.88
        );

        Map<String, Object> leafLevel = Map.of(
                "level", 0,
                "source", "leaf-1",
                "text", leafSummaries != null && !leafSummaries.isEmpty()
                        ? leafSummaries.get(0).get("text")
                        : "Conductor is an orchestration platform for distributed apps.",
                "relevance", 0.94
        );

        List<Map<String, Object>> multiLevelContext = List.of(rootLevel, clusterLevel, leafLevel);

        System.out.println("  [tree_search] Found context at " + multiLevelContext.size() + " levels");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("multiLevelContext", multiLevelContext);
        return result;
    }
}
