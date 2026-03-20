package errorclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ec_handle_error — logs the error type and details from the
 * upstream API call, then completes with a handled=true flag.
 */
public class ErrorHandlerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_handle_error";
    }

    @Override
    public TaskResult execute(Task task) {
        Object errorType = task.getInputData().get("errorType");
        Object error = task.getInputData().get("error");
        Object httpStatus = task.getInputData().get("httpStatus");

        String errorTypeStr = errorType != null ? errorType.toString() : "unknown";
        String errorStr = error != null ? error.toString() : "no details";
        String httpStatusStr = httpStatus != null ? httpStatus.toString() : "N/A";

        System.out.println("  [ec_handle_error] Handling error:");
        System.out.println("    errorType:  " + errorTypeStr);
        System.out.println("    error:      " + errorStr);
        System.out.println("    httpStatus: " + httpStatusStr);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("errorType", errorTypeStr);
        result.getOutputData().put("error", errorStr);
        result.getOutputData().put("httpStatus", httpStatusStr);

        return result;
    }
}
