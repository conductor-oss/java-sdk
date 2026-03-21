package deploymentrollback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IdentifyVersionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rb_identify_version";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [version] Last stable version: 2.4.3, deployed 3 days ago");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("identify_version", true);
        result.addOutputData("processed", true);
        return result;
    }
}
