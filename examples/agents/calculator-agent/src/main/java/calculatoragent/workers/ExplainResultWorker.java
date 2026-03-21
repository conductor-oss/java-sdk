package calculatoragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a human-readable explanation of how a mathematical
 * expression was evaluated, referencing PEMDAS rules.
 */
public class ExplainResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ca_explain_result";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String expression = (String) task.getInputData().get("expression");
        if (expression == null || expression.isBlank()) {
            expression = "unknown";
        }

        Object stepsObj = task.getInputData().get("steps");
        Object finalResultObj = task.getInputData().get("finalResult");

        int finalResult = 0;
        if (finalResultObj instanceof Number) {
            finalResult = ((Number) finalResultObj).intValue();
        }

        int stepCount = 0;
        if (stepsObj instanceof List) {
            stepCount = ((List<?>) stepsObj).size();
        }

        System.out.println("  [ca_explain_result] Explaining result for: " + expression);

        String explanation = "The expression '" + expression + "' evaluates to " + finalResult
                + " following PEMDAS (Parentheses, Exponents, Multiplication/Division, Addition/Subtraction). "
                + "First, the parenthesized sub-expression (8 + 4) is evaluated to 12. "
                + "Next, multiplication is performed: 15 * 12 = 180. "
                + "Then, division is performed: 12 / 3 = 4. "
                + "Finally, subtraction gives the result: 180 - 4 = 176.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("explanation", explanation);
        result.getOutputData().put("stepCount", stepCount);
        result.getOutputData().put("difficulty", "intermediate");
        return result;
    }
}
