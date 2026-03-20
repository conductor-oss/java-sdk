package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TransformWorker implements Worker {
    @Override public String getTaskDefName() { return "dm_transform"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [transform] Transforming schema from v1 to v2");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("dataPath", "/tmp/transformed-data");
        r.getOutputData().put("recordCount", 50000);
        return r;
    }
}
