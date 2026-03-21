package disasterrecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] DR region healthy, RTO: 8 minutes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("recovered", true);
        result.addOutputData("rtoMinutes", 8);
        return result;
    }
}
