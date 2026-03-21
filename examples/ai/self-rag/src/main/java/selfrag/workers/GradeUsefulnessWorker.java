package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Grades the generated answer for usefulness.
 * score = 0.88. If score >= 0.7 AND halScore >= 0.7, verdict = "pass".
 * Returns {score, verdict}.
 */
public class GradeUsefulnessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_grade_usefulness";
    }

    @Override
    public TaskResult execute(Task task) {
        double score = 0.88;
        double halScore = ((Number) task.getInputData().get("halScore")).doubleValue();
        String verdict = (score >= 0.7 && halScore >= 0.7) ? "pass" : "fail";
        System.out.println("  [grade-usefulness] Score: " + score + ", verdict: " + verdict);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("verdict", verdict);
        return result;
    }
}
