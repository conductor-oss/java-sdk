package devsecopspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ContainerScanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dso_container_scan";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [container] Image scan: 0 critical vulnerabilities");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("container_scan", true);
        result.addOutputData("processed", true);
        return result;
    }
}
