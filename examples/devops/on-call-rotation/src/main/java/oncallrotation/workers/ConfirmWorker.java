package oncallrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oc_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [confirm] New on-call confirmed status");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("confirm", true);
        return result;
    }
}
