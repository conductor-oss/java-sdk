package leadnurturing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

public class SendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nur_send";
    }

    @Override
    public TaskResult execute(Task task) {
        String deliveryId = "DLV-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        System.out.println("  [send] Nurture email sent -> " + deliveryId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("deliveryId", deliveryId);
        result.getOutputData().put("channel", "email");
        result.getOutputData().put("sentAt", Instant.now().toString());
        return result;
    }
}
