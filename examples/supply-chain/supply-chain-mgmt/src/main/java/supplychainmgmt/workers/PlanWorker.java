package supplychainmgmt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Creates a production plan based on product and quantity.
 * Input: product, quantity
 * Output: planId, materials
 */
public class PlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "scm_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String product = (String) task.getInputData().get("product");
        Object qtyObj = task.getInputData().get("quantity");
        int quantity = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;

        List<Map<String, Object>> materials = List.of(
                Map.of("name", "steel", "qty", quantity * 2),
                Map.of("name", "electronics", "qty", quantity),
                Map.of("name", "packaging", "qty", quantity)
        );

        System.out.println("  [plan] Production plan for " + quantity + "x " + product);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("planId", "PLN-651");
        result.getOutputData().put("materials", materials);
        return result;
    }
}
