package chatbotorchestration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class UnderstandIntentWorker implements Worker {
    @Override public String getTaskDefName() { return "cbo_understand_intent"; }
    @Override public TaskResult execute(Task task) {
        String msg = ((String) task.getInputData().getOrDefault("message", "")).toLowerCase();
        String intent = msg.contains("refund") ? "request_refund" : msg.contains("cancel") ? "cancel_subscription" : "general_inquiry";
        System.out.println("  [intent] Detected intent: " + intent + " (confidence: 0.94)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("intent", intent);
        result.getOutputData().put("confidence", 0.94);
        result.getOutputData().put("entities", Map.of("product", "Pro Plan", "amount", "$49.99"));
        return result;
    }
}
