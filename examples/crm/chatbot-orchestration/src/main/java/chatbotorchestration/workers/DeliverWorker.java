package chatbotorchestration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {
    @Override public String getTaskDefName() { return "cbo_deliver"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deliver] Response delivered to " + task.getInputData().get("userId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("channel", "webchat");
        result.getOutputData().put("latencyMs", 320);
        return result;
    }
}
