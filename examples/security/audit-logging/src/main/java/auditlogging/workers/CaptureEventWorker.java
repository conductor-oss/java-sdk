package auditlogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CaptureEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "al_capture_event";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [capture] admin@example.com delete user/12345");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("capture_eventId", "CAPTURE_EVENT-1481");
        result.addOutputData("success", true);
        return result;
    }
}
