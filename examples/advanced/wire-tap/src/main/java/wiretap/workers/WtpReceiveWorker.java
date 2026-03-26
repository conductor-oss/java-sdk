package wiretap.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtpReceiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtp_receive";
    }

    @Override
    public TaskResult execute(Task task) {
        Object msg = task.getInputData().getOrDefault("message", java.util.Map.of());
        System.out.println("  [receive] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("message", msg);
        return result;
    }
}