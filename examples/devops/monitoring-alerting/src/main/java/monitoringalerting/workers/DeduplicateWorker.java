package monitoringalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeduplicateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ma_deduplicate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dedup] 1 unique from 5 raw alerts");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("unique", true);
        return result;
    }
}
