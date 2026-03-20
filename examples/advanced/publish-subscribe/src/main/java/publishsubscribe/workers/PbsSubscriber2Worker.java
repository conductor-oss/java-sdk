package publishsubscribe.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PbsSubscriber2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbs_subscriber_2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sub-2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("received", true);
        result.getOutputData().put("subscriber", task.getInputData().getOrDefault("subscriberName", "sub2"));
        result.getOutputData().put("action", "sent_notification");
        return result;
    }
}