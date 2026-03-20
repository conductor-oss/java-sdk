package ragqualitygates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that checks the faithfulness of a generated answer against the
 * source documents. Evaluates 3 fixed claims, computes a faithfulness
 * score, and compares against a threshold of 0.8.
 * Returns faithfulnessScore, claims, threshold, and decision (pass/fail).
 */
public class CheckFaithfulnessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_check_faithfulness";
    }

    @Override
    public TaskResult execute(Task task) {
        String answer = (String) task.getInputData().get("answer");
        System.out.println("  [check_faithfulness] Checking faithfulness of generated answer");

        List<Map<String, Object>> claims = List.of(
                Map.of("claim", "Conductor orchestrates microservices", "supported", true),
                Map.of("claim", "Workers poll for tasks independently", "supported", true),
                Map.of("claim", "Workflow versioning allows safe rollouts", "supported", true)
        );

        long supportedCount = claims.stream()
                .filter(c -> Boolean.TRUE.equals(c.get("supported")))
                .count();
        double faithfulnessScore = (double) supportedCount / claims.size();
        double threshold = 0.8;
        String decision = faithfulnessScore >= threshold ? "pass" : "fail";

        System.out.println("  [check_faithfulness] Faithfulness: " + String.format("%.2f", faithfulnessScore)
                + " (" + supportedCount + "/" + claims.size() + " claims supported) -> " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("faithfulnessScore", faithfulnessScore);
        result.getOutputData().put("claims", claims);
        result.getOutputData().put("threshold", threshold);
        result.getOutputData().put("decision", decision);
        return result;
    }
}
