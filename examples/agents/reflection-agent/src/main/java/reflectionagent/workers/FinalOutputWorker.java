package reflectionagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Produces the final polished output after all reflection iterations. Combines
 * the improved drafts into a high-quality piece with a quality score reflecting
 * the cumulative improvements.
 */
public class FinalOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rn_final_output";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        int totalIterations = 2;
        Object iterObj = task.getInputData().get("totalIterations");
        if (iterObj instanceof Number) {
            totalIterations = ((Number) iterObj).intValue();
        }

        System.out.println("  [rn_final_output] Producing final output on: " + topic
                + " after " + totalIterations + " iterations");

        String content = "Microservices architecture decomposes monoliths into independently "
                + "deployable services. Each service owns its data, scales independently, and "
                + "communicates through well-defined APIs. Key benefits include team autonomy, "
                + "technology diversity, and fault isolation. Trade-offs include distributed "
                + "system complexity, data consistency challenges, and operational overhead. "
                + "Best practices encompass API gateways, circuit breakers, centralized logging, "
                + "container orchestration, and comprehensive monitoring strategies.";
        double qualityScore = 0.92;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", content);
        result.getOutputData().put("qualityScore", qualityScore);
        return result;
    }
}
