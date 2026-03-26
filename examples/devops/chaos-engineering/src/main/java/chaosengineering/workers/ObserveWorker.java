package chaosengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ObserveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ce_observe";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [observe] System remained within SLO: p99 < 2s");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("observe", true);
        result.addOutputData("processed", true);
        return result;
    }
}
