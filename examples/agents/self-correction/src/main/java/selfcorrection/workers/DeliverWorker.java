package selfcorrection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Delivers the final code.
 * Accepts the code, test pass count, and an optional wasFixed flag.
 */
public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sc_deliver";
    }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        if (code == null || code.isBlank()) {
            code = "";
        }

        Object testsPassedObj = task.getInputData().get("testsPassed");
        int testsPassed = 0;
        if (testsPassedObj instanceof Number) {
            testsPassed = ((Number) testsPassedObj).intValue();
        }

        Object wasFixedObj = task.getInputData().get("wasFixed");
        boolean wasFixed = Boolean.TRUE.equals(wasFixedObj);

        System.out.println("  [sc_deliver] Delivering code (wasFixed=" + wasFixed + ")...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveredCode", code);
        result.getOutputData().put("deliveryStatus", "success");
        return result;
    }
}
