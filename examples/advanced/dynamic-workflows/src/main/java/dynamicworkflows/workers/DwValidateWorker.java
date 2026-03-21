package dynamicworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validate step in a dynamic pipeline.
 * Input: stepId, stepType, config, previousOutput
 * Output: result, stepType
 */
public class DwValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String stepId = (String) task.getInputData().get("stepId");
        String config = (String) task.getInputData().get("config");
        if (stepId == null) stepId = "unknown";

        System.out.println("  [validate] Executing validation step (config: " + config + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "validate_complete");
        result.getOutputData().put("stepType", "validation");
        return result;
    }
}
