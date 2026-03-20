package restaurantmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SeatingWorker implements Worker {
    @Override public String getTaskDefName() { return "rst_seating"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [seat] Seating reservation");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("tableId", "T-12");
        result.addOutputData("section", "patio");
        result.addOutputData("seated", true);
        return result;
    }
}
