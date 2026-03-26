package eventreplaytesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sets up an isolated sandbox environment for replay testing.
 * Input: testSuiteId
 * Output: sandboxId, ready
 */
public class SetupSandboxWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rt_setup_sandbox";
    }

    @Override
    public TaskResult execute(Task task) {
        String testSuiteId = (String) task.getInputData().get("testSuiteId");
        if (testSuiteId == null) {
            testSuiteId = "unknown";
        }

        System.out.println("  [rt_setup_sandbox] Setting up sandbox for suite: " + testSuiteId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sandboxId", "sandbox_fixed_001");
        result.getOutputData().put("ready", true);
        return result;
    }
}
