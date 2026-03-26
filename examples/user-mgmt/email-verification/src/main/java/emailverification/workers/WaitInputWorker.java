package emailverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * perform  waiting for user to submit their verification code.
 * Input: expectedCode, userId
 * Output: submittedCode
 */
public class WaitInputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emv_wait_input";
    }

    @Override
    public TaskResult execute(Task task) {
        String expectedCode = (String) task.getInputData().get("expectedCode");

        System.out.println("  [wait] User submitted verification code");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submittedCode", expectedCode);
        return result;
    }
}
