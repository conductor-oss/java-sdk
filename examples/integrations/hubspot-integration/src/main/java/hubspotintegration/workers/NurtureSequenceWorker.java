package hubspotintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enrolls a contact in a nurture sequence.
 * Input: contactId, ownerId, segment
 * Output: sequenceName, enrolledAt, stepsCount
 */
public class NurtureSequenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hs_nurture_sequence";
    }

    @Override
    public TaskResult execute(Task task) {
        String contactId = (String) task.getInputData().get("contactId");
        String segment = (String) task.getInputData().get("segment");
        String sequenceName = segment + "-welcome-series";
        System.out.println("  [nurture] Enrolled contact " + contactId + " in \"" + sequenceName + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sequenceName", "" + sequenceName);
        result.getOutputData().put("enrolledAt", "" + java.time.Instant.now().toString());
        result.getOutputData().put("stepsCount", 5);
        return result;
    }
}
