package edgeorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EorCollectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eor_collect";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [collect] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collected", true);
        result.getOutputData().put("nodeCount", 2);
        result.getOutputData().put("totalRecords", 5500);
        return result;
    }
}