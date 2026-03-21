package edgeorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EorDispatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eor_dispatch";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [dispatch] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dispatched", true);
        result.getOutputData().put("assignments", java.util.List.of(java.util.Map.of("node","edge-1","partition","part_1"), java.util.Map.of("node","edge-2","partition","part_2")));
        return result;
    }
}