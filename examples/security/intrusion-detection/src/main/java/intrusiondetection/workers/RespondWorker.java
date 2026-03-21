package intrusiondetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RespondWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "id_respond";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [respond] Blocked IP, notified security team");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("respond", true);
        return result;
    }
}
