package costoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplySavingsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_apply_savings";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [apply] Right-sized 8 instances, terminated 10 idle resources");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("apply_savings", true);
        return result;
    }
}
