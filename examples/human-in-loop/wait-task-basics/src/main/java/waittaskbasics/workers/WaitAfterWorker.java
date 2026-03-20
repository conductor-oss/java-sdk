package waittaskbasics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for wait_after — processes data after the WAIT task completes.
 *
 * Takes requestId and approval from the WAIT task output and returns
 * a completion message: "completed-{requestId}".
 */
public class WaitAfterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wait_after";
    }

    @Override
    public TaskResult execute(Task task) {
        Object requestId = task.getInputData().get("requestId");
        Object approval = task.getInputData().get("approval");

        String reqIdStr = requestId != null ? requestId.toString() : "unknown";

        System.out.println("  [wait_after] Processing after wait — requestId=" + reqIdStr
                + ", approval=" + approval);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "completed-" + reqIdStr);

        return result;
    }
}
