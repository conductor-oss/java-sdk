package dripcampaign.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class EnrollWorker implements Worker {
    @Override public String getTaskDefName() { return "drp_enroll"; }

    @Override
    public TaskResult execute(Task task) {
        String enrollId = "ENR-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        System.out.println("  [enroll] Contact " + task.getInputData().get("contactId") + " enrolled in campaign " + task.getInputData().get("campaignId") + " -> " + enrollId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrollmentId", enrollId);
        result.getOutputData().put("enrolledAt", Instant.now().toString());
        return result;
    }
}
