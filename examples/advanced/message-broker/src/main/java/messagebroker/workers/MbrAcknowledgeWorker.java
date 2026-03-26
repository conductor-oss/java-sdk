package messagebroker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MbrAcknowledgeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mbr_acknowledge";
    }

    @Override
    public TaskResult execute(Task task) {
        Object del = task.getInputData().get("delivered");
        boolean delivered = Boolean.TRUE.equals(del) || "true".equals(String.valueOf(del));
        System.out.println("  [ack] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("acknowledged", delivered);
        return result;
    }
}