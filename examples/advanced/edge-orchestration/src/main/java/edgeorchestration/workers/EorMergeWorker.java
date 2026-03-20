package edgeorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EorMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eor_merge";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [merge] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedResult", "edge_data_merged");
        result.getOutputData().put("complete", true);
        return result;
    }
}