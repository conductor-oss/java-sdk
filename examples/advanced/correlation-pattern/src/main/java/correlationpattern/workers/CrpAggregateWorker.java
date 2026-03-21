package correlationpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CrpAggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crp_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [aggregate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregatedResults", java.util.List.of(java.util.Map.of("correlationId","TXN-001","messageCount",2,"complete",true)));
        return result;
    }
}