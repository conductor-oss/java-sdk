package gracefulshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GshStopWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gsh_stop";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [stop] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stopped", true);
        result.getOutputData().put("cleanShutdown", true);
        return result;
    }
}