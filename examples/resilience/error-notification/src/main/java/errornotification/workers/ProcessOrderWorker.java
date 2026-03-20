package errornotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for en_process_order -- processes an order.
 *
 * If the input contains shouldFail=true, the task returns FAILED status.
 * Otherwise it succeeds with result "order-processed".
 */
public class ProcessOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_process_order";
    }

    @Override
    public TaskResult execute(Task task) {
        Object shouldFailInput = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailInput)
                || "true".equals(String.valueOf(shouldFailInput));

        TaskResult result = new TaskResult(task);

        if (shouldFail) {
            System.out.println("  [en_process_order] Order processing FAILED (shouldFail=true)");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Order processing failed");
        } else {
            System.out.println("  [en_process_order] Order processed successfully");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", "order-processed");
        }

        return result;
    }
}
