package reflectionagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Incorporates reflection feedback to improve the draft. Returns the revised
 * content and a flag indicating the feedback was applied.
 */
public class ImproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rn_improve";
    }

    @Override
    public TaskResult execute(Task task) {
        String feedback = (String) task.getInputData().get("feedback");
        if (feedback == null || feedback.isBlank()) {
            feedback = "no feedback provided";
        }

        int iteration = 1;
        Object iterObj = task.getInputData().get("iteration");
        if (iterObj instanceof Number) {
            iteration = ((Number) iterObj).intValue();
        }

        System.out.println("  [rn_improve] Improving draft (iteration " + iteration
                + ") based on feedback: " + feedback);

        String content = "Improved draft incorporating feedback — added depth on trade-offs and patterns";
        boolean applied = true;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("applied", applied);
        return result;
    }
}
