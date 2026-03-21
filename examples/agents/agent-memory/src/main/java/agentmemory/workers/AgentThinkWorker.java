package agentmemory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Analyzes user message in the context of conversation history and user profile.
 * Returns thoughts, relevantFacts, and factsRecalledCount.
 */
public class AgentThinkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_agent_think";
    }

    @Override
    public TaskResult execute(Task task) {
        String userMessage = (String) task.getInputData().get("userMessage");

        if (userMessage == null || userMessage.isBlank()) {
            userMessage = "";
        }

        System.out.println("  [am_agent_think] Thinking about: " + userMessage);

        String thoughts = "The user has shown a progression from general ML interest to neural networks "
                + "and is now asking about transformers. This follows a natural learning path. "
                + "I should build on their existing knowledge of neural networks to explain "
                + "the transformer architecture.";

        List<String> relevantFacts = List.of(
                "User has intermediate expertise in machine learning",
                "Previous conversation covered neural networks and deep learning basics",
                "User learns best with conceptual explanations followed by details"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("thoughts", thoughts);
        result.getOutputData().put("relevantFacts", relevantFacts);
        result.getOutputData().put("factsRecalledCount", 3);
        return result;
    }
}
