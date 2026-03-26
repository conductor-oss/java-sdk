package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Creative reasoning path. Proposes an innovative edge-computing approach
 * using CDN-based logic, serverless edge functions, and predictive pre-warming.
 */
public class PathBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_path_b";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "";
        }
        String approach = (String) task.getInputData().get("approach");
        if (approach == null || approach.isBlank()) {
            approach = "creative";
        }

        System.out.println("  [tt_path_b] Exploring creative path for: " + problem);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution",
                "Edge computing with CDN-based logic, serverless functions at edge nodes, predictive pre-warming");
        result.getOutputData().put("approach", approach);
        result.getOutputData().put("confidence", 0.72);
        return result;
    }
}
