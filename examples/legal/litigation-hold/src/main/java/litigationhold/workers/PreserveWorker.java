package litigationhold.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PreserveWorker implements Worker {
    @Override public String getTaskDefName() { return "lth_preserve"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [preserve] Preserving collected data");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("preservationId", "PRSV-" + System.currentTimeMillis());
        result.getOutputData().put("preserved", true);
        return result;
    }
}
