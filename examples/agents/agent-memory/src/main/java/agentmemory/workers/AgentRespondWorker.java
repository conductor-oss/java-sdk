package agentmemory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates the final response based on thoughts, relevant facts, and updated memory.
 * Returns the response string, confidence score, and token count.
 */
public class AgentRespondWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_agent_respond";
    }

    @Override
    public TaskResult execute(Task task) {
        String userMessage = (String) task.getInputData().get("userMessage");

        if (userMessage == null || userMessage.isBlank()) {
            userMessage = "";
        }

        System.out.println("  [am_agent_respond] Generating response for: " + userMessage);

        String response = "Building on our earlier discussion of neural networks, transformers are "
                + "a powerful architecture that replaced recurrence with self-attention. Instead of "
                + "processing tokens sequentially, transformers attend to all positions in a sequence "
                + "simultaneously, enabling much faster training and better capture of long-range "
                + "dependencies. The key innovation is the multi-head attention mechanism, which "
                + "allows the model to focus on different parts of the input for different purposes.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        result.getOutputData().put("confidence", 0.92);
        result.getOutputData().put("tokensUsed", 87);
        return result;
    }
}
