package syncexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * SIMPLE worker that adds two numbers.
 *
 * Takes input { a, b } and returns { sum: a + b }.
 * Demonstrates the simplest possible worker for sync execution demos.
 */
public class AddWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sync_add";
    }

    @Override
    public TaskResult execute(Task task) {
        Number a = (Number) task.getInputData().get("a");
        Number b = (Number) task.getInputData().get("b");

        double numA = a != null ? a.doubleValue() : 0;
        double numB = b != null ? b.doubleValue() : 0;
        double sum = numA + numB;

        System.out.println("  [WORKER] sync_add: " + numA + " + " + numB + " = " + sum);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sum", sum);
        return result;
    }
}
