package oncallrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HandoffWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oc_handoff";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [handoff] Handed off 2 active incidents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("handoff", true);
        result.addOutputData("processed", true);
        return result;
    }
}
