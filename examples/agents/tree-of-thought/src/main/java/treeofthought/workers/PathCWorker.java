package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Empirical reasoning path. Proposes a data-driven solution based on traffic
 * analysis: regional clusters with geo-routing and caching top queries.
 */
public class PathCWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_path_c";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "";
        }
        String approach = (String) task.getInputData().get("approach");
        if (approach == null || approach.isBlank()) {
            approach = "empirical";
        }

        System.out.println("  [tt_path_c] Exploring empirical path for: " + problem);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution",
                "Based on traffic analysis: 80% requests from 3 regions, deploy regional clusters with geo-routing, cache top 1000 queries");
        result.getOutputData().put("approach", approach);
        result.getOutputData().put("confidence", 0.91);
        return result;
    }
}
