package gradingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GradeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grd_grade";
    }

    @Override
    public TaskResult execute(Task task) {
        String submissionId = (String) task.getInputData().get("submissionId");
        int score = 88;

        System.out.println("  [grade] " + submissionId + ": scored " + score + "/100");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("rubric", "standard");
        result.getOutputData().put("feedback", "Good work on algorithms section");
        return result;
    }
}
