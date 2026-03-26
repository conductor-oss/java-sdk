package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CutoverWorker implements Worker {
    @Override public String getTaskDefName() { return "dm_cutover"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cutover] Switching to new database");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("cutover", true);
        return r;
    }
}
