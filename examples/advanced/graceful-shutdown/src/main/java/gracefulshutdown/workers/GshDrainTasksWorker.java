package gracefulshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GshDrainTasksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gsh_drain_tasks";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [drain] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("drainedCount", 12);
        result.getOutputData().put("inFlightTasks", java.util.List.of(java.util.Map.of("taskId", "t-001", "status", "running"), java.util.Map.of("taskId", "t-002", "status", "running")));
        return result;
    }
}