package gradingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grd_review";
    }

    @Override
    public TaskResult execute(Task task) {
        Object scoreObj = task.getInputData().get("score");
        int finalScore = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;

        System.out.println("  [review] Grade review complete - final score: " + finalScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalScore", finalScore);
        result.getOutputData().put("reviewed", true);
        return result;
    }
}
