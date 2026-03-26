package requestreply.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RqrDeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rqr_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        Object m = task.getInputData().get("matched");
        boolean matched = Boolean.TRUE.equals(m) || "true".equals(String.valueOf(m));
        System.out.println("  [deliver] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", matched);
        return result;
    }
}