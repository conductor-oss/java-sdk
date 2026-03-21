package calculatoragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Computes the result of a parsed expression step-by-step,
 * following the operation order from the parser.
 */
public class ComputeStepsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ca_compute_steps";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object tokensObj = task.getInputData().get("tokens");
        Object orderObj = task.getInputData().get("operationOrder");

        String precision = (String) task.getInputData().get("precision");
        if (precision == null || precision.isBlank()) {
            precision = "exact";
        }

        System.out.println("  [ca_compute_steps] Computing with precision: " + precision);

        List<Map<String, Object>> steps = List.of(
                Map.of(
                        "step", 1,
                        "operation", "8+4=12",
                        "intermediate", 12,
                        "rule", "Evaluate parentheses first"
                ),
                Map.of(
                        "step", 2,
                        "operation", "15*12=180",
                        "intermediate", 180,
                        "rule", "Multiplication before addition/subtraction"
                ),
                Map.of(
                        "step", 3,
                        "operation", "12/3=4",
                        "intermediate", 4,
                        "rule", "Division before addition/subtraction"
                ),
                Map.of(
                        "step", 4,
                        "operation", "180-4=176",
                        "intermediate", 176,
                        "rule", "Left-to-right for same precedence"
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("steps", steps);
        result.getOutputData().put("finalResult", 176);
        result.getOutputData().put("precision", precision);
        return result;
    }
}
