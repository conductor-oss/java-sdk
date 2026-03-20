package chaosengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class InjectFailureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ce_inject_failure";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [inject] Injected 500ms latency on 50% of requests");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("inject_failure", true);
        result.addOutputData("processed", true);
        return result;
    }
}
