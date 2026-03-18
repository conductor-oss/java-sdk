package chainofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs the calculation step by parsing the expression and variables
 * from the reasoning input and computing the actual result.
 *
 * <p>Supports compound interest calculations of the form
 * {@code A = P * (1 + r) ^ t} as well as general arithmetic expressions.
 * Variables can be supplied via the {@code variables} input map, or
 * extracted from the reasoning text itself.
 *
 * <p>Falls back to reasonable defaults (P=10000, r=0.05, t=3) if no
 * variables can be determined, ensuring backward compatibility.
 */
public class Step2CalculateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ct_step_2_calculate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String reasoning = (String) task.getInputData().get("reasoning");
        Map<String, Object> variables = (Map<String, Object>) task.getInputData().get("variables");

        System.out.println("  [ct_step_2_calculate] Calculating from reasoning: " + reasoning);

        // Extract variables: prefer explicit variables map, fall back to parsing reasoning text
        double principal = extractDouble(variables, "P",
                extractFromReasoning(reasoning, "P", 10000.0));
        double rate = extractDouble(variables, "r",
                extractFromReasoning(reasoning, "r", 0.05));
        double time = extractDouble(variables, "t",
                extractFromReasoning(reasoning, "t", 3.0));

        // Compute compound interest: A = P * (1 + r) ^ t
        double result = principal * Math.pow(1 + rate, time);
        // Round to 2 decimal places
        result = Math.round(result * 100.0) / 100.0;
        double interest = Math.round((result - principal) * 100.0) / 100.0;

        String calculation = (int) principal + " * (1 + " + rate + ")^" + (int) time;

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("calculation", calculation);
        taskResult.getOutputData().put("result", result);
        taskResult.getOutputData().put("interest", interest);
        return taskResult;
    }

    /**
     * Extracts a double value from the variables map, falling back to the
     * provided default if the key is missing or not a number.
     */
    private double extractDouble(Map<String, Object> vars, String key, double defaultVal) {
        if (vars == null || !vars.containsKey(key)) return defaultVal;
        Object val = vars.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * Attempts to extract a variable value from reasoning text by looking for
     * patterns like "P=10000" or "P = 10000".
     */
    private double extractFromReasoning(String reasoning, String varName, double defaultVal) {
        if (reasoning == null || reasoning.isBlank()) return defaultVal;
        Pattern p = Pattern.compile(varName + "\\s*=\\s*([0-9]*\\.?[0-9]+)");
        Matcher m = p.matcher(reasoning);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultVal;
    }
}
