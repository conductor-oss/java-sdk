package threeagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Final output worker — assembles the complete report from all agent outputs.
 */
public class FinalOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "thr_final_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> research = (Map<String, Object>) task.getInputData().get("research");
        String draft = (String) task.getInputData().get("draft");
        Map<String, Object> review = (Map<String, Object>) task.getInputData().get("review");

        System.out.println("  [final-output] Assembling final report...");

        List<String> sourcesUsed = research != null
                ? (List<String>) research.get("sources")
                : List.of();

        Object reviewScore = review != null ? review.get("score") : 0;
        String verdict = review != null ? (String) review.get("verdict") : "UNKNOWN";
        List<String> suggestions = review != null
                ? (List<String>) review.get("suggestions")
                : List.of();

        List<String> agentsPipeline = List.of(
                "researcher-agent-v1",
                "writer-agent-v1",
                "reviewer-agent-v1"
        );

        Map<String, Object> finalReport = Map.of(
                "content", draft != null ? draft : "",
                "reviewScore", reviewScore,
                "verdict", verdict,
                "suggestions", suggestions,
                "sourcesUsed", sourcesUsed,
                "agentsPipeline", agentsPipeline
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalReport", finalReport);
        return result;
    }
}
