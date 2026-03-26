package performancereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ManagerEvalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_manager_eval";
    }

    @Override
    public TaskResult execute(Task task) {
        String managerId = (String) task.getInputData().get("managerId");
        System.out.println("  [manager-eval] Manager " + managerId + " assessment: 4.2/5");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rating", 4.2);
        result.getOutputData().put("feedback", "Consistently exceeds expectations");
        return result;
    }
}
