package educationenrollment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Reviews a student application and assigns a score based on GPA.
 * Input: applicationId, gpa
 * Output: score, reviewer
 */
public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edu_review";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicationId = (String) task.getInputData().get("applicationId");
        double gpa = 0;
        Object gpaObj = task.getInputData().get("gpa");
        if (gpaObj instanceof Number) {
            gpa = ((Number) gpaObj).doubleValue();
        } else if (gpaObj instanceof String) {
            try { gpa = Double.parseDouble((String) gpaObj); } catch (NumberFormatException ignored) {}
        }

        int score = (int) Math.min(100, Math.round(gpa * 25));
        System.out.println("  [review] " + applicationId + ": GPA " + gpa + " -> score " + score);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("reviewer", "admissions-committee");
        return result;
    }
}
