package reflectionagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Reflects on the current draft and provides constructive feedback along with
 * a quality score. The feedback varies by iteration to perform progressive
 * improvement.
 */
public class ReflectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rn_reflect";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        int iteration = 1;
        Object iterObj = task.getInputData().get("iteration");
        if (iterObj instanceof Number) {
            iteration = ((Number) iterObj).intValue();
        }

        System.out.println("  [rn_reflect] Reflecting on draft (iteration " + iteration + ") for: " + topic);

        String feedback;
        double currentScore;

        if (iteration <= 1) {
            feedback = "Too shallow — needs concrete benefits, trade-offs, and real-world examples";
            currentScore = 0.5;
        } else {
            feedback = "Better depth but could improve on deployment patterns and monitoring strategies";
            currentScore = 0.7;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("feedback", feedback);
        result.getOutputData().put("currentScore", currentScore);
        return result;
    }
}
