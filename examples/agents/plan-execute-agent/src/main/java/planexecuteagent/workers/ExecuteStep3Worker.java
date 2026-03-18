package planexecuteagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes step 3 of the plan: generates strategic recommendations.
 */
public class ExecuteStep3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pe_execute_step_3";
    }

    @Override
    public TaskResult execute(Task task) {
        String step = (String) task.getInputData().get("step");
        if (step == null || step.isBlank()) {
            step = "";
        }

        Object stepIndexObj = task.getInputData().get("stepIndex");
        int stepIndex = 2;
        if (stepIndexObj instanceof Number) {
            stepIndex = ((Number) stepIndexObj).intValue();
        }

        String previousResult = (String) task.getInputData().get("previousResult");
        if (previousResult == null || previousResult.isBlank()) {
            previousResult = "";
        }

        System.out.println("  [pe_execute_step_3] Executing step " + stepIndex + ": " + step);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Recommend prioritizing API platform (ROI: capacity-planning%) followed by enterprise tier (ROI: 210%)");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
