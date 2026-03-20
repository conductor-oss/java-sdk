package reflectionagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates an initial draft on a given topic. Produces a shallow first pass
 * with a low quality score, setting the stage for reflection and improvement.
 */
public class InitialGenerationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rn_initial_generation";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        System.out.println("  [rn_initial_generation] Generating initial draft on: " + topic);

        String content = "Microservices architecture splits applications into small services. "
                + "Each service runs independently. They communicate via APIs.";
        double qualityScore = 0.5;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("qualityScore", qualityScore);
        return result;
    }
}
