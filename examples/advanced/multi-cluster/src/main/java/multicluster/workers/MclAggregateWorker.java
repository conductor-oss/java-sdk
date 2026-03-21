package multicluster.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MclAggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mcl_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        int east = task.getInputData().get("eastCount") instanceof Number ? ((Number) task.getInputData().get("eastCount")).intValue() : 0;
        int west = task.getInputData().get("westCount") instanceof Number ? ((Number) task.getInputData().get("westCount")).intValue() : 0;
        System.out.println("  [aggregate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalProcessed", east + west);
        result.getOutputData().put("clusters", java.util.List.of("us-east-1", "us-west-2"));
        return result;
    }
}