package twoagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Writer agent that produces a draft about a given topic.
 * Takes topic, tone, and systemPrompt as inputs and returns a draft string,
 * wordCount, and model identifier.
 */
public class WriterAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tap_writer_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().getOrDefault("topic", "technology");
        String tone = (String) task.getInputData().getOrDefault("tone", "professional");
        String systemPrompt = (String) task.getInputData().getOrDefault("systemPrompt",
                "You are a skilled writer.");

        String draft = "The world of " + topic + " continues to evolve at a remarkable pace. "
                + "Innovations in this field are reshaping how we live, work, and interact. "
                + "As we look to the future, the possibilities seem endless and full of promise. "
                + "Written in a " + tone + " tone as instructed by: " + systemPrompt;

        System.out.println("  [writer-agent] Drafted content about '" + topic
                + "' in " + tone + " tone");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("draft", draft);
        result.getOutputData().put("wordCount", 35);
        result.getOutputData().put("model", "writer-agent-v1");
        return result;
    }
}
