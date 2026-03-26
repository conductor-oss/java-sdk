package devsecopspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScaScanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dso_sca_scan";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sca] Dependency scan: 0 critical, 1 high vulnerability");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("sca_scan", true);
        result.addOutputData("processed", true);
        return result;
    }
}
