package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives a problem statement and passes it through for the parallel
 * exploration paths. Acts as the root of the tree-of-thought.
 */
public class DefineProblemWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_define_problem";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "No problem defined";
        }

        System.out.println("  [tt_define_problem] Problem: " + problem);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("problem", problem);
        return result;
    }
}
