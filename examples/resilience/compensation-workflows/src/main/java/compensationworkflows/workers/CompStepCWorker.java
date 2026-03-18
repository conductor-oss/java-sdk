package compensationworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for comp_step_c -- sends a notification.
 * Fails if the input failAtStep equals "C", otherwise succeeds
 * with { result: "notification-C-sent" }.
 */
public class CompStepCWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_step_c";
    }

    @Override
    public TaskResult execute(Task task) {
        Object failAtStep = task.getInputData().get("failAtStep");

        TaskResult result = new TaskResult(task);

        if ("C".equals(failAtStep)) {
            System.out.println("  [Step C] FAILED -- external service error");
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("External service unavailable");
        } else {
            System.out.println("  [Step C] Executing -- notification sent");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", "notification-C-sent");
        }
        return result;
    }
}
