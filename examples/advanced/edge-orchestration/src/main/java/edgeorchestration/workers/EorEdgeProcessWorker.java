package edgeorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EorEdgeProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eor_edge_process";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [edge-process] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", java.util.List.of(java.util.Map.of("node","edge-1","records",3000), java.util.Map.of("node","edge-2","records",2500)));
        return result;
    }
}