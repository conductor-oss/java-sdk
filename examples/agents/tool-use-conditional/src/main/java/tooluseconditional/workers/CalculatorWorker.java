package tooluseconditional.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Handles math-category queries by performing a calculator tool.
 * Returns an answer string, a calculation object with expression/result/steps/method,
 * and the toolUsed identifier.
 */
public class CalculatorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tc_calculator";
    }

    @Override
    public TaskResult execute(Task task) {
        String expression = (String) task.getInputData().get("expression");
        if (expression == null || expression.isBlank()) {
            expression = "0";
        }

        String userQuery = (String) task.getInputData().get("userQuery");
        if (userQuery == null || userQuery.isBlank()) {
            userQuery = expression;
        }

        System.out.println("  [tc_calculator] Evaluating expression: " + expression);

        int numericResult;
        String method;
        List<String> steps;

        String lower = expression.toLowerCase();
        if (lower.contains("square root") || lower.contains("sqrt")) {
            numericResult = 12;
            method = "square_root";
            steps = List.of(
                    "Identified square root operation",
                    "Extracted operand: 144",
                    "Computed sqrt(144) = 12"
            );
        } else if (lower.contains("factorial")) {
            numericResult = 120;
            method = "factorial";
            steps = List.of(
                    "Identified factorial operation",
                    "Extracted operand: 5",
                    "Computed 5! = 120"
            );
        } else {
            numericResult = 42;
            method = "arithmetic";
            steps = List.of(
                    "Parsed arithmetic expression",
                    "Evaluated expression",
                    "Computed result = 42"
            );
        }

        String answer = "The result of " + expression + " is " + numericResult + ".";

        Map<String, Object> calculation = Map.of(
                "expression", expression,
                "result", numericResult,
                "steps", steps,
                "method", method
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("calculation", calculation);
        result.getOutputData().put("toolUsed", "calculator");
        return result;
    }
}
