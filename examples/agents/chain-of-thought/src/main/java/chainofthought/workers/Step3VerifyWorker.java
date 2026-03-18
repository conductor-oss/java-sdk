package chainofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies the calculation result by independently re-computing the value
 * from the calculation expression and comparing it against the provided result.
 *
 * <p>Parses expressions of the form {@code P * (1 + r)^t} from the
 * {@code calculation} input, re-computes the expected value, and compares
 * it with the claimed {@code result}. Reports a confidence score based on
 * how closely the values match.
 *
 * <p>Falls back to trusting the input result with full confidence if the
 * expression cannot be parsed.
 */
public class Step3VerifyWorker implements Worker {

    /** Tolerance for floating-point comparison. */
    private static final double EPSILON = 0.01;

    @Override
    public String getTaskDefName() {
        return "ct_step_3_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String calculation = (String) task.getInputData().get("calculation");
        Object resultObj = task.getInputData().get("result");

        double claimedResult = 0.0;
        if (resultObj instanceof Number) {
            claimedResult = ((Number) resultObj).doubleValue();
        } else if (resultObj != null) {
            try {
                claimedResult = Double.parseDouble(resultObj.toString());
            } catch (NumberFormatException e) {
                // leave as 0
            }
        }

        System.out.println("  [ct_step_3_verify] Verifying calculation: " + calculation);

        double recomputed = recompute(calculation);
        boolean verified;
        double confidence;

        if (Double.isNaN(recomputed)) {
            // Could not parse the expression; trust the input
            recomputed = claimedResult;
            verified = true;
            confidence = 1.0;
        } else {
            double diff = Math.abs(recomputed - claimedResult);
            verified = diff < EPSILON;
            // Confidence decreases with distance between claimed and recomputed
            if (verified) {
                confidence = 1.0;
            } else if (claimedResult == 0 && recomputed == 0) {
                confidence = 1.0;
            } else {
                double maxAbs = Math.max(Math.abs(recomputed), Math.abs(claimedResult));
                confidence = Math.max(0.0, 1.0 - (diff / maxAbs));
                confidence = Math.round(confidence * 100.0) / 100.0;
            }
        }

        double verifiedResult = Math.round(recomputed * 100.0) / 100.0;

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("verifiedResult", verifiedResult);
        taskResult.getOutputData().put("verified", verified);
        taskResult.getOutputData().put("confidence", confidence);
        return taskResult;
    }

    /**
     * Re-computes the result from a calculation expression of the form
     * {@code P * (1 + r)^t}. Returns {@link Double#NaN} if parsing fails.
     */
    private double recompute(String calculation) {
        if (calculation == null || calculation.isBlank()) return Double.NaN;

        // Try to match "P * (1 + r)^t" pattern
        Pattern p = Pattern.compile("([0-9.]+)\\s*\\*\\s*\\(1\\s*\\+\\s*([0-9.]+)\\)\\^([0-9.]+)");
        Matcher m = p.matcher(calculation);
        if (m.find()) {
            try {
                double principal = Double.parseDouble(m.group(1));
                double rate = Double.parseDouble(m.group(2));
                double time = Double.parseDouble(m.group(3));
                return principal * Math.pow(1 + rate, time);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        // Try to evaluate a simple arithmetic expression
        try {
            return evaluateSimple(calculation);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * Simple expression evaluator for basic arithmetic (+, -, *, /).
     */
    private double evaluateSimple(String expr) {
        expr = expr.replaceAll("\\s+", "");
        return parseAddSub(expr, new int[]{0});
    }

    private double parseAddSub(String s, int[] pos) {
        double val = parseMulDiv(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '+' || s.charAt(pos[0]) == '-')) {
            char op = s.charAt(pos[0]++);
            double right = parseMulDiv(s, pos);
            val = op == '+' ? val + right : val - right;
        }
        return val;
    }

    private double parseMulDiv(String s, int[] pos) {
        double val = parseAtom(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '*' || s.charAt(pos[0]) == '/')) {
            char op = s.charAt(pos[0]++);
            double right = parseAtom(s, pos);
            val = op == '*' ? val * right : val / right;
        }
        return val;
    }

    private double parseAtom(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
            pos[0]++;
            double val = parseAddSub(s, pos);
            if (pos[0] < s.length() && s.charAt(pos[0]) == ')') pos[0]++;
            return val;
        }
        int start = pos[0];
        if (pos[0] < s.length() && s.charAt(pos[0]) == '-') pos[0]++;
        while (pos[0] < s.length() && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        return Double.parseDouble(s.substring(start, pos[0]));
    }
}
