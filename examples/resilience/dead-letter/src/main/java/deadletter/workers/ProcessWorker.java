package deadletter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for dl_process — processes data based on mode input.
 *
 * If mode="fail", returns FAILED with an error message.
 * Otherwise, returns COMPLETED with processed result.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dl_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String mode = "success";
        Object modeInput = task.getInputData().get("mode");
        if (modeInput instanceof String) {
            mode = (String) modeInput;
        }

        String data = "";
        Object dataInput = task.getInputData().get("data");
        if (dataInput instanceof String) {
            data = (String) dataInput;
        }

        System.out.println("  [dl_process] mode=" + mode + ", data=" + data);

        TaskResult result = new TaskResult(task);

        if ("fail".equals(mode)) {
            System.out.println("  [dl_process] Triggering intentional failure for dead-letter demo");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Processing failed for data: " + data);
            result.getOutputData().put("mode", mode);
        } else {
            System.out.println("  [dl_process] Processing succeeded");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", "Processed: " + data);
            result.getOutputData().put("mode", mode);
        }

        return result;
    }
}
