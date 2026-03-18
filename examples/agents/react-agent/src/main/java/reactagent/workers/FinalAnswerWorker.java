package reactagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Produces the final answer after all ReAct iterations are complete.
 * Returns a deterministic answer with a confidence score.
 */
public class FinalAnswerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rx_final_answer";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        Object totalIterationsObj = task.getInputData().get("totalIterations");
        int totalIterations = 3;
        if (totalIterationsObj instanceof Number) {
            totalIterations = ((Number) totalIterationsObj).intValue();
        }

        System.out.println("  [rx_final_answer] Generating final answer after " + totalIterations + " iterations");

        String answer = "The world population is approximately 8.1 billion people as of 2024, confirmed by multiple sources.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("confidence", 0.95);
        return result;
    }
}
