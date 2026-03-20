package chatbotorchestration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.Map;

public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "cbo_receive"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receive] Message from " + task.getInputData().get("userId") + ": \"" + task.getInputData().get("message") + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("context", Map.of("previousTurns", 3, "topic", "billing"));
        result.getOutputData().put("receivedAt", Instant.now().toString());
        return result;
    }
}
