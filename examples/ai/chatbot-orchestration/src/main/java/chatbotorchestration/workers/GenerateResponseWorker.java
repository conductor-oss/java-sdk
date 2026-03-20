package chatbotorchestration.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class GenerateResponseWorker implements Worker {
    @Override public String getTaskDefName() { return "cbo_generate_response"; }
    @Override public TaskResult execute(Task task) {
        String intent = (String) task.getInputData().getOrDefault("intent", "general_inquiry");
        Map<String, String> responses = Map.of(
            "request_refund", "I can help with your refund for the Pro Plan ($49.99). Let me process that for you.",
            "cancel_subscription", "I understand you'd like to cancel. Let me walk you through the process.",
            "general_inquiry", "I'd be happy to help! Could you tell me more about your question?"
        );
        String response = responses.getOrDefault(intent, responses.get("general_inquiry"));
        System.out.println("  [generate] Response generated for intent: " + intent);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        result.getOutputData().put("model", "gpt-4");
        result.getOutputData().put("tokensUsed", 85);
        return result;
    }
}
