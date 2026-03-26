package modelregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MrgVersionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrg_version";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [version] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("version", "3.1.0");
        result.getOutputData().put("previousVersion", "3.0.2");
        return result;
    }
}