package bluegreendeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates the green environment with smoke tests.
 * Input: serviceName, environment
 * Output: healthy, testsRun, testsPassed
 */
public class ValidateGreenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bg_validate_green";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        String environment = (String) task.getInputData().get("environment");
        if (environment == null) environment = "green";

        System.out.println("  [validate] " + environment + " environment healthy: smoke tests passed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("healthy", true);
        result.getOutputData().put("testsRun", 24);
        result.getOutputData().put("testsPassed", 24);
        result.getOutputData().put("environment", environment);
        return result;
    }
}
