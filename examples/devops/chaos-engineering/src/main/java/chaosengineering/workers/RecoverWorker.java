package chaosengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecoverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ce_recover";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [recover] Fault removed, system nominal");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("recover", true);
        return result;
    }
}
