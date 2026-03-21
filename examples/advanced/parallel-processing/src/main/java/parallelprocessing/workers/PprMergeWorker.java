package parallelprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PprMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ppr_merge";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [merge] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedResult", java.util.List.of(2,4,6,8,10,12,14,16,18));
        result.getOutputData().put("totalProcessed", 9);
        return result;
    }
}