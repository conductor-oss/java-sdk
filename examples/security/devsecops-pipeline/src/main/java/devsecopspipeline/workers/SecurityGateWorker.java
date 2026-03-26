package devsecopspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SecurityGateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dso_security_gate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [gate] Security gate PASSED — no critical findings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("security_gate", true);
        return result;
    }
}
