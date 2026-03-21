package requestreply.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RqrCorrelateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rqr_correlate";
    }

    @Override
    public TaskResult execute(Task task) {
        String origId = String.valueOf(task.getInputData().getOrDefault("originalCorrelationId", ""));
        String respId = String.valueOf(task.getInputData().getOrDefault("responseCorrelationId", ""));
        boolean matched = origId.equals(respId);
        System.out.println("  [correlate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("matched", matched);
        result.getOutputData().put("correlatedResponse", task.getInputData().get("response"));
        return result;
    }
}