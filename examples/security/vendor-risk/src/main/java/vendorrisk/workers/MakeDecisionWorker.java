package vendorrisk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MakeDecisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_make_decision";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [decision] Approved with conditions: require encryption at rest");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("make_decision", true);
        return result;
    }
}
