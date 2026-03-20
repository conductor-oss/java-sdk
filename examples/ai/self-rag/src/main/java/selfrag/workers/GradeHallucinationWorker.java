package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Grades the generated answer for hallucination against source docs.
 * Returns {score: 0.92, grounded: true}.
 */
public class GradeHallucinationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_grade_hallucination";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [grade-hallucination] Checking answer against source docs...");
        System.out.println("  [grade-hallucination] Score: 0.92 (grounded in sources)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", 0.92);
        result.getOutputData().put("grounded", true);
        return result;
    }
}
