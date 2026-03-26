package bluegreendeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys the new version to the green environment.
 * Input: serviceName, imageTag
 * Output: deployed, environment
 */
public class DeployGreenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bg_deploy_green";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        String imageTag = (String) task.getInputData().get("imageTag");
        if (imageTag == null) imageTag = "unknown:latest";

        System.out.println("  [deploy] Deploying " + imageTag + " to green environment");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deployed", true);
        result.getOutputData().put("environment", "green");
        result.getOutputData().put("serviceName", serviceName);
        result.getOutputData().put("imageTag", imageTag);
        return result;
    }
}
