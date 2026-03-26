package dynamicworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publish step in a dynamic pipeline.
 * Input: stepId, stepType, config, previousOutput
 * Output: result, stepType
 */
public class DwPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String config = (String) task.getInputData().get("config");
        if (config == null) config = "{}";

        System.out.println("  [publish] Executing publish step (config: " + config + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "publish_complete");
        result.getOutputData().put("stepType", "publish");
        return result;
    }
}
