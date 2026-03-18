package planexecuteagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Executes step 1 of the plan: gathers market data and competitor analysis.
 */
public class ExecuteStep1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pe_execute_step_1";
    }

    @Override
    public TaskResult execute(Task task) {
        String step = (String) task.getInputData().get("step");
        if (step == null || step.isBlank()) {
            step = "";
        }

        Object stepIndexObj = task.getInputData().get("stepIndex");
        int stepIndex = 0;
        if (stepIndexObj instanceof Number) {
            stepIndex = ((Number) stepIndexObj).intValue();
        }

        System.out.println("  [pe_execute_step_1] Executing step " + stepIndex + ": " + step);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result",
                "Collected data on 5 competitors; market size estimated at $4.2B");
        result.getOutputData().put("status", "complete");
        return result;
    }
}
