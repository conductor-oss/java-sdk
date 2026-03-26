package debateagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Sets up the debate topic and defines the two sides (PRO and CON).
 */
public class SetTopicWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "da_set_topic";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "Technology in modern society";
        }

        System.out.println("  [da_set_topic] Setting debate topic: " + topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("topic", topic);
        result.getOutputData().put("sides", List.of("PRO", "CON"));
        return result;
    }
}
