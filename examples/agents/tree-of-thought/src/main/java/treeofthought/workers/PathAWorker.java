package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Analytical reasoning path. Proposes a conventional, well-proven solution
 * using load balancers, auto-scaling groups, and multi-AZ deployment.
 */
public class PathAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_path_a";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "";
        }
        String approach = (String) task.getInputData().get("approach");
        if (approach == null || approach.isBlank()) {
            approach = "analytical";
        }

        System.out.println("  [tt_path_a] Exploring analytical path for: " + problem);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution",
                "Use load balancer with auto-scaling groups, deploy across 3 AZs, implement health checks with 30s intervals");
        result.getOutputData().put("approach", approach);
        result.getOutputData().put("confidence", 0.85);
        return result;
    }
}
