package canarydeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployCanaryWorker implements Worker {

    @Override public String getTaskDefName() { return "cd_deploy_canary"; }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";
        String newVersion = (String) task.getInputData().get("newVersion");
        if (newVersion == null) newVersion = "unknown";

        System.out.println("  [deploy] Canary: " + serviceName + ":" + newVersion);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deployed", true);
        result.getOutputData().put("serviceName", serviceName);
        result.getOutputData().put("newVersion", newVersion);
        return result;
    }
}
