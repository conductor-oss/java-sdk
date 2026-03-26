package supplychainmgmt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Manufactures the product.
 * Input: product, quantity, sourcedMaterials
 * Output: batchId, unitsProduced
 */
public class MakeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "scm_make";
    }

    @Override
    public TaskResult execute(Task task) {
        String product = (String) task.getInputData().get("product");
        Object qtyObj = task.getInputData().get("quantity");
        int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;

        System.out.println("  [make] Manufacturing " + quantity + "x " + product);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batchId", "BATCH-651-001");
        result.getOutputData().put("unitsProduced", quantity);
        return result;
    }
}
