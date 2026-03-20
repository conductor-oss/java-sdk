package publishsubscribe.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PbsSubscriber3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbs_subscriber_3";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sub-3] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("received", true);
        result.getOutputData().put("subscriber", task.getInputData().getOrDefault("subscriberName", "sub3"));
        result.getOutputData().put("action", "wrote_audit_log");
        return result;
    }
}