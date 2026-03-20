package qualitygate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for qg_deploy task -- deploys the application after QA sign-off.
 *
 * Outputs:
 *   - deployed: true
 */
public class DeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [qg_deploy] Deploying application...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deployed", true);

        System.out.println("  [qg_deploy] Deployment complete.");
        return result;
    }
}
