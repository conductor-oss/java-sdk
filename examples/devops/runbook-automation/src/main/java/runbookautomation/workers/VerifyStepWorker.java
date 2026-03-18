package runbookautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Verifies the results of runbook step execution by running real verification
 * commands. Checks:
 *   - Whether all steps completed successfully (exit code 0)
 *   - Whether any steps timed out
 *   - Runs additional verification commands (e.g., checking system health post-remediation)
 *
 * Input:
 *   - stepResult (Map): output from ExecuteStepWorker
 *
 * Output:
 *   - verified (boolean): whether all verifications passed
 *   - checks (List): individual verification check results
 *   - checksTotal (int): total number of checks
 *   - checksPassed (int): number of checks that passed
 */
public class VerifyStepWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_verify_step";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> stepResult = task.getInputData().get("stepResult") instanceof Map
                ? (Map<String, Object>) task.getInputData().get("stepResult") : new LinkedHashMap<>();

        System.out.println("[ra_verify_step] Verifying step execution results...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> checks = new ArrayList<>();
        boolean allPassed = true;

        // Check 1: Verify all steps completed (no failures)
        int failureCount = toInt(stepResult.get("failureCount"), 0);
        int successCount = toInt(stepResult.get("successCount"), 0);
        boolean noFailures = failureCount == 0;
        checks.add(Map.of(
                "check", "step-completion",
                "passed", noFailures,
                "detail", successCount + " succeeded, " + failureCount + " failed"
        ));
        if (!noFailures) allPassed = false;
        System.out.println("  Step completion: " + (noFailures ? "PASS" : "FAIL")
                + " (" + successCount + "/" + (successCount + failureCount) + " succeeded)");

        // Check 2: Verify no timeouts
        List<Map<String, Object>> results = stepResult.get("results") instanceof List
                ? (List<Map<String, Object>>) stepResult.get("results") : new ArrayList<>();
        boolean noTimeouts = results.stream()
                .noneMatch(r -> Boolean.TRUE.equals(r.get("timedOut")));
        checks.add(Map.of(
                "check", "no-timeouts",
                "passed", noTimeouts,
                "detail", noTimeouts ? "No steps timed out" : "One or more steps timed out"
        ));
        if (!noTimeouts) allPassed = false;
        System.out.println("  Timeouts: " + (noTimeouts ? "PASS" : "FAIL"));

        // Check 3: Run a real system health check to verify post-execution state
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "uptime && echo OK");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            boolean completed = proc.waitFor(5, TimeUnit.SECONDS);
            boolean healthOk = completed && proc.exitValue() == 0 && output.contains("OK");

            checks.add(Map.of(
                    "check", "system-health",
                    "passed", healthOk,
                    "detail", healthOk ? "System responsive after execution" : "System health check failed"
            ));
            if (!healthOk) allPassed = false;
            System.out.println("  System health: " + (healthOk ? "PASS" : "FAIL"));
        } catch (Exception e) {
            checks.add(Map.of(
                    "check", "system-health",
                    "passed", false,
                    "detail", "Could not verify: " + e.getMessage()
            ));
            allPassed = false;
        }

        // Check 4: Verify reasonable execution time
        long totalDurationMs = toLong(stepResult.get("totalDurationMs"), 0);
        boolean timeReasonable = totalDurationMs < 300_000; // Under 5 minutes
        checks.add(Map.of(
                "check", "execution-time",
                "passed", timeReasonable,
                "detail", "Total execution: " + totalDurationMs + "ms"
        ));
        if (!timeReasonable) allPassed = false;
        System.out.println("  Execution time: " + (timeReasonable ? "PASS" : "FAIL")
                + " (" + totalDurationMs + "ms)");

        int passed = (int) checks.stream().filter(c -> Boolean.TRUE.equals(c.get("passed"))).count();

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verified", allPassed);
        result.addOutputData("checks", checks);
        result.addOutputData("checksTotal", checks.size());
        result.addOutputData("checksPassed", passed);
        result.addOutputData("verifiedAt", Instant.now().toString());

        return result;
    }

    private int toInt(Object val, int def) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private long toLong(Object val, long def) {
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return def; }
    }
}
