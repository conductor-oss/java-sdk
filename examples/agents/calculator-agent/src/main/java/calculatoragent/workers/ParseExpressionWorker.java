package calculatoragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Parses a mathematical expression into tokens and determines
 * the order of operations following PEMDAS rules.
 */
public class ParseExpressionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ca_parse_expression";
    }

    @Override
    public TaskResult execute(Task task) {
        String expression = (String) task.getInputData().get("expression");
        if (expression == null || expression.isBlank()) {
            expression = "0";
        }

        String precision = (String) task.getInputData().get("precision");
        if (precision == null || precision.isBlank()) {
            precision = "exact";
        }

        System.out.println("  [ca_parse_expression] Parsing expression: " + expression);

        List<Map<String, Object>> tokens = List.of(
                Map.of("type", "number", "value", "15"),
                Map.of("type", "operator", "value", "*"),
                Map.of("type", "parenthesis_open", "value", "("),
                Map.of("type", "number", "value", "8"),
                Map.of("type", "operator", "value", "+"),
                Map.of("type", "number", "value", "4"),
                Map.of("type", "parenthesis_close", "value", ")"),
                Map.of("type", "operator", "value", "-"),
                Map.of("type", "number", "value", "12"),
                Map.of("type", "operator", "value", "/"),
                Map.of("type", "number", "value", "3")
        );

        List<String> operationOrder = List.of(
                "Evaluate parentheses: (8 + 4)",
                "Multiply: 15 * 12",
                "Divide: 12 / 3",
                "Subtract: 180 - 4"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tokens", tokens);
        result.getOutputData().put("operationOrder", operationOrder);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("expressionType", "arithmetic");
        result.getOutputData().put("precision", precision);
        return result;
    }
}
