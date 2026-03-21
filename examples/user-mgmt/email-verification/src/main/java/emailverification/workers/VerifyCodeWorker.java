package emailverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies the submitted code against the expected code.
 * Input: submittedCode, expectedCode
 * Output: codeMatch
 */
public class VerifyCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emv_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        Object submitted = task.getInputData().get("submittedCode");
        Object expected = task.getInputData().get("expectedCode");
        boolean match = submitted != null && submitted.equals(expected);

        System.out.println("  [verify] Code verification: " + (match ? "match" : "mismatch"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("codeMatch", match);
        return result;
    }
}
