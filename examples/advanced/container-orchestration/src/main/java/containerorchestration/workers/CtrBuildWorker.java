package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Builds a container image for the given service and tag.
 * Input: serviceName, imageTag
 * Output: imageUri, sizeBytes, layers
 */
public class CtrBuildWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctr_build";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";
        String imageTag = (String) task.getInputData().get("imageTag");
        if (imageTag == null) imageTag = "latest";

        String imageUri = "registry.io/" + serviceName + ":" + imageTag;
        System.out.println("  [build] Building image: " + imageUri);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("imageUri", imageUri);
        result.getOutputData().put("sizeBytes", 245000000);
        result.getOutputData().put("layers", 12);
        return result;
    }
}
