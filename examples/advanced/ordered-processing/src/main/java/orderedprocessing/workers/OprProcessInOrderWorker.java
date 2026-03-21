package orderedprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OprProcessInOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opr_process_in_order";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedOrder", java.util.List.of(1, 2, 3, 4, 5));
        result.getOutputData().put("processedCount", 5);
        return result;
    }
}