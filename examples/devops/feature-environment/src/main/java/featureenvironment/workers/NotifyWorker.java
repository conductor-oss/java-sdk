package featureenvironment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [notify] Posted preview link to PR");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("notify", true);
        return result;
    }
}
