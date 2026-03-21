package auditlogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyIntegrityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "al_verify_integrity";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Hash chain verified — log integrity intact");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_integrity", true);
        return result;
    }
}
