package failureworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes the main workflow task.
 * Takes a shouldFail flag — if true, fails the task to trigger the failure workflow.
 * If false, returns a success result.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fw_process";
    }

    @Override
    public TaskResult execute(Task task) {
        Object shouldFailObj = task.getInputData().get("shouldFail");
        boolean shouldFail = toBoolean(shouldFailObj);

        System.out.println("  [fw_process] Processing... shouldFail=" + shouldFail);

        TaskResult result = new TaskResult(task);

        if (shouldFail) {
            System.out.println("  [fw_process] Performing failure!");
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Intentional failure: shouldFail was true");
            result.getOutputData().put("status", "FAILED");
            result.getOutputData().put("message", "Processing failed as requested");
        } else {
            System.out.println("  [fw_process] Processing succeeded.");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("status", "SUCCESS");
            result.getOutputData().put("message", "Processing completed successfully");
        }

        return result;
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }
}
