package educationenrollment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Decides whether to admit the student based on review score.
 * Input: applicationId, reviewScore
 * Output: admitted, studentId
 */
public class AdmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edu_admit";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicationId = (String) task.getInputData().get("applicationId");
        int reviewScore = 0;
        Object scoreObj = task.getInputData().get("reviewScore");
        if (scoreObj instanceof Number) {
            reviewScore = ((Number) scoreObj).intValue();
        }

        boolean admitted = reviewScore >= 75;
        System.out.println("  [admit] " + applicationId + ": " + (admitted ? "ADMITTED" : "WAITLISTED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("admitted", admitted);
        result.getOutputData().put("studentId", "STU-671-001");
        return result;
    }
}
