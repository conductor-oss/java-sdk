package customerjourney.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Generates optimization recommendations based on journey insights.
 * Input: insights
 * Output: recommendations (list of strings)
 */
public class OptimizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cjy_optimize";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [optimize] 3 optimization recommendations generated");

        List<String> recommendations = List.of(
                "Add retargeting at consideration stage",
                "Increase email frequency",
                "Personalize landing pages"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recommendations", recommendations);
        return result;
    }
}
