package voicebot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class GenerateWorker implements Worker {
    @Override public String getTaskDefName() { return "vb_generate"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> entities = (Map<String, Object>) task.getInputData().getOrDefault("entities", Map.of());
        String orderNum = (String) entities.getOrDefault("orderNumber", "unknown");
        String text = "Your order number " + orderNum + " is currently in transit and expected to arrive by Thursday.";
        System.out.println("  [generate] Response: \"" + text + "\"");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responseText", text);
        result.getOutputData().put("tokensUsed", 32);
        return result;
    }
}
