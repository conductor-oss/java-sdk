package publishsubscribe.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PbsPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbs_publish";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [publish] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String eventId = "EVT-" + Long.toString(System.currentTimeMillis(), 36);
        result.getOutputData().put("eventId", eventId);
        result.getOutputData().put("published", true);
        return result;
    }
}