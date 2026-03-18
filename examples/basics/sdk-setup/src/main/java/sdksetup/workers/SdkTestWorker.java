package sdksetup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Simple worker for the SDK setup smoke test.
 * Takes a "check" input and returns a result string confirming the SDK is working.
 */
public class SdkTestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sdk_test_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String check = (String) task.getInputData().get("check");
        if (check == null || check.isBlank()) {
            check = "default";
        }

        String resultMessage = "SDK check '" + check + "' passed — conductor-client 5.0.1 is working";
        System.out.println("  [sdk_test_task worker] " + resultMessage);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", resultMessage);
        return result;
    }
}
