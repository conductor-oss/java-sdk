package planexecuteagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes step 2 of the plan: analyzes trends and identifies opportunities.
 */
public class ExecuteStep2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pe_execute_step_2";
    }

    @Override
    public TaskResult execute(Task task) {
        String step = (String) task.getInputData().get("step");
        if (step == null || step.isBlank()) {
            step = "";
        }

        Object stepIndexObj = task.getInputData().get("stepIndex");
        int stepIndex = 1;
        if (stepIndexObj instanceof Number) {
            stepIndex = ((Number) stepIndexObj).intValue();
        }

        String previousResult = (String) task.getInputData().get("previousResult");
        if (previousResult == null || previousResult.isBlank()) {
            previousResult = "";
        }

        System.out.println("  [pe_execute_step_2] Executing step " + stepIndex + ": " + step);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Identified 3 growth opportunities: API platform, enterprise tier, international expansion");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
