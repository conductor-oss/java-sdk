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
 * Executes real shell commands from the runbook steps. Runs each command via
 * ProcessBuilder with a configurable timeout, captures stdout and stderr,
 * and reports the exit code and execution time.
 *
 * Security: Commands are executed via /bin/sh -c to support pipes and
 * redirections. The timeout is enforced to prevent runaway processes.
 *
 * Input:
 *   - runbookId (String): runbook identifier for correlation
 *   - steps (List): optional list of steps from LoadRunbookWorker
 *   - command (String): optional single command to execute
 *   - timeoutSeconds (int): optional timeout (default: 30)
 *
 * Output:
 *   - executed (boolean): whether execution completed
 *   - results (List): per-step execution results
 *   - totalDurationMs (long): total execution time
 *   - successCount (int): number of steps that succeeded
 *   - failureCount (int): number of steps that failed
 */
public class ExecuteStepWorker implements Worker {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public String getTaskDefName() {
        return "ra_execute_step";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String runbookId = task.getInputData().get("runbookId") != null
                ? String.valueOf(task.getInputData().get("runbookId")) : "unknown";

        System.out.println("[ra_execute_step] Executing steps for runbook " + runbookId);

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> stepResults = new ArrayList<>();
        long totalStartMs = System.currentTimeMillis();
        int successCount = 0, failureCount = 0;

        // Get steps from input - could be a list of steps or a single command
        List<Map<String, Object>> steps = null;
        if (task.getInputData().get("steps") instanceof List) {
            steps = (List<Map<String, Object>>) task.getInputData().get("steps");
        }

        if (steps == null || steps.isEmpty()) {
            // Single command mode
            String command = task.getInputData().get("command") != null
                    ? String.valueOf(task.getInputData().get("command")) : "echo 'No command provided'";
            int timeout = toInt(task.getInputData().get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS);

            steps = List.of(Map.of("name", "single-command", "command", command, "timeoutSeconds", timeout));
        }

        for (Map<String, Object> step : steps) {
            String stepName = String.valueOf(step.getOrDefault("name", "unnamed"));
            String command = String.valueOf(step.getOrDefault("command", "echo 'empty'"));
            int timeout = toInt(step.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS);

            System.out.println("  Step: " + stepName + " | Command: " + command);

            Map<String, Object> stepResult = executeCommand(stepName, command, timeout);
            stepResults.add(stepResult);

            int exitCode = ((Number) stepResult.get("exitCode")).intValue();
            if (exitCode == 0) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        long totalDurationMs = System.currentTimeMillis() - totalStartMs;

        System.out.println("  Completed: " + successCount + " succeeded, " + failureCount + " failed"
                + " (" + totalDurationMs + "ms total)");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("executed", true);
        result.addOutputData("results", stepResults);
        result.addOutputData("totalDurationMs", totalDurationMs);
        result.addOutputData("successCount", successCount);
        result.addOutputData("failureCount", failureCount);
        result.addOutputData("stepCount", steps.size());

        return result;
    }

    private Map<String, Object> executeCommand(String name, String command, int timeoutSeconds) {
        Map<String, Object> stepResult = new LinkedHashMap<>();
        stepResult.put("name", name);
        stepResult.put("command", command);
        stepResult.put("startedAt", Instant.now().toString());

        long startMs = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(false);

            Process proc = pb.start();

            // Capture stdout
            String stdout;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }

            // Capture stderr
            String stderr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startMs;

            if (!completed) {
                proc.destroyForcibly();
                stepResult.put("exitCode", -1);
                stepResult.put("stdout", stdout);
                stepResult.put("stderr", "Command timed out after " + timeoutSeconds + "s");
                stepResult.put("timedOut", true);
                stepResult.put("durationMs", durationMs);
                System.out.println("    TIMEOUT after " + timeoutSeconds + "s");
            } else {
                int exitCode = proc.exitValue();
                stepResult.put("exitCode", exitCode);
                stepResult.put("stdout", truncate(stdout, 5000));
                stepResult.put("stderr", truncate(stderr, 2000));
                stepResult.put("timedOut", false);
                stepResult.put("durationMs", durationMs);

                if (exitCode == 0) {
                    System.out.println("    OK (" + durationMs + "ms)");
                } else {
                    System.out.println("    FAILED (exit " + exitCode + ", " + durationMs + "ms)");
                }
            }
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            stepResult.put("exitCode", -1);
            stepResult.put("stdout", "");
            stepResult.put("stderr", "Execution error: " + e.getMessage());
            stepResult.put("timedOut", false);
            stepResult.put("durationMs", durationMs);
            System.out.println("    ERROR: " + e.getMessage());
        }

        stepResult.put("completedAt", Instant.now().toString());
        return stepResult;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return "...(truncated)...\n" + s.substring(s.length() - maxLen);
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
