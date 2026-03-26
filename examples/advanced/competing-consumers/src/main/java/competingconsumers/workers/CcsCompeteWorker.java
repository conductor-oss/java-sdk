package competingconsumers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CcsCompeteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccs_compete";
    }

    @Override
    public TaskResult execute(Task task) {
        int count = task.getInputData().get("consumerCount") instanceof Number ? ((Number) task.getInputData().get("consumerCount")).intValue() : 3;
        String winner = "consumer-" + ((int)(Math.random() * count) + 1);
        System.out.println("  [compete] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("winner", winner);
        result.getOutputData().put("competitors", count);
        result.getOutputData().put("claimTimestamp", java.time.Instant.now().toString());
        return result;
    }
}