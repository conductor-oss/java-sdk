package reactagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Reasons about the question and decides what action to take next.
 * Produces a thought, an action type, and a query for the action.
 * Uses a fixed 3-iteration cycle of thoughts and actions.
 */
public class ReasonWorker implements Worker {

    private static final String[] THOUGHTS = {
            "I need to search for the current world population to answer this question.",
            "Let me verify this information with another source to ensure accuracy.",
            "I now have enough information from multiple sources to provide a confident answer."
    };

    private static final String[] ACTIONS = {
            "search",
            "search",
            "synthesize"
    };

    private static final String[] QUERIES = {
            "current world population 2024",
            "world population estimate 2024 verification",
            "compile final answer from collected evidence"
    };

    @Override
    public String getTaskDefName() {
        return "rx_reason";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        Object iterationObj = task.getInputData().get("iteration");
        int iteration = 1;
        if (iterationObj instanceof Number) {
            iteration = ((Number) iterationObj).intValue();
        }

        int index = Math.max(0, Math.min(iteration - 1, 2));

        String thought = THOUGHTS[index];
        String action = ACTIONS[index];
        String query = QUERIES[index];

        System.out.println("  [rx_reason] Iteration " + iteration + ": " + thought);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("thought", thought);
        result.getOutputData().put("action", action);
        result.getOutputData().put("query", query);
        return result;
    }
}
