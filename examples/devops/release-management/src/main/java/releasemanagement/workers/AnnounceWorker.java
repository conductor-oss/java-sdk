package releasemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnnounceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rm_announce";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [announce] Release notes published, stakeholders notified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("announce", true);
        return result;
    }
}
