package servicemigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReplicateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_replicate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [replicate] Service replicated to target");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("replicate", true);
        result.addOutputData("processed", true);
        return result;
    }
}
