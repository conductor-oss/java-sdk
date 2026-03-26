package dynamicworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Transform step in a dynamic pipeline.
 * Input: stepId, stepType, config, previousOutput
 * Output: result, stepType
 */
public class DwTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_transform";
    }

    @Override
    public TaskResult execute(Task task) {
        String config = (String) task.getInputData().get("config");
        if (config == null) config = "{}";

        System.out.println("  [transform] Executing transformation step (config: " + config + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "transform_complete");
        result.getOutputData().put("stepType", "transformation");
        return result;
    }
}
