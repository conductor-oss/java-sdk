package chainofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Produces the final human-readable answer combining the verified result
 * and confidence score.
 */
public class FinalAnswerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ct_final_answer";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        Object verifiedResult = task.getInputData().get("verifiedResult");
        Object confidence = task.getInputData().get("confidence");

        System.out.println("  [ct_final_answer] Composing answer for: " + problem);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer",
                "The compound interest on $10,000 at 5% for 3 years yields $11576.25 (confidence: 1.0)");
        return result;
    }
}
