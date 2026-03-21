package disasterrecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_detect";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [detect] Primary us-east-1 failure confirmed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("failed", true);
        return result;
    }
}
