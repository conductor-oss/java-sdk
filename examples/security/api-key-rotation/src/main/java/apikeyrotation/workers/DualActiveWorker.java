package apikeyrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DualActiveWorker implements Worker {

    @Override public String getTaskDefName() { return "akr_dual_active"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dual] Both old and new keys active during transition");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dual_active", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
