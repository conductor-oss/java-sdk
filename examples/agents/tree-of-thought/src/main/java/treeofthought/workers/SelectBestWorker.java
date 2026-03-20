package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Selects and returns the best reasoning path and its solution
 * as the final output of the tree-of-thought workflow.
 */
public class SelectBestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_select_best";
    }

    @Override
    public TaskResult execute(Task task) {
        String bestPath = (String) task.getInputData().get("bestPath");
        if (bestPath == null || bestPath.isBlank()) {
            bestPath = "unknown";
        }
        String bestSolution = (String) task.getInputData().get("bestSolution");
        if (bestSolution == null || bestSolution.isBlank()) {
            bestSolution = "No solution available";
        }
        String evaluation = (String) task.getInputData().get("evaluation");
        if (evaluation == null || evaluation.isBlank()) {
            evaluation = "";
        }

        System.out.println("  [tt_select_best] Selected path: " + bestPath);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedPath", bestPath);
        result.getOutputData().put("solution", bestSolution);
        return result;
    }
}
