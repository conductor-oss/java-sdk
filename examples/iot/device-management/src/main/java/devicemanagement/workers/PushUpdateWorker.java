package devicemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Checks and pushes firmware updates. Real version comparison.
 */
public class PushUpdateWorker implements Worker {
    private static final String LATEST_FIRMWARE = "2.5.1";

    @Override public String getTaskDefName() { return "dev_push_update"; }

    @Override public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().get("deviceId");
        String currentFirmware = (String) task.getInputData().get("currentFirmware");
        if (deviceId == null) deviceId = "unknown";
        if (currentFirmware == null) currentFirmware = "0.0.0";

        // Real version comparison
        int comparison = compareVersions(currentFirmware, LATEST_FIRMWARE);
        String updateStatus;
        if (comparison >= 0) {
            updateStatus = "up_to_date";
        } else {
            updateStatus = "update_available";
        }

        System.out.println("  [update] " + deviceId + ": v" + currentFirmware + " -> "
                + (comparison < 0 ? "v" + LATEST_FIRMWARE + " available" : "up to date"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updateStatus", updateStatus);
        result.getOutputData().put("currentVersion", currentFirmware);
        result.getOutputData().put("latestVersion", LATEST_FIRMWARE);
        result.getOutputData().put("nextCheckAt", Instant.now().plus(24, ChronoUnit.HOURS).toString());
        return result;
    }

    /** Compare semantic versions: returns < 0 if v1 < v2, 0 if equal, > 0 if v1 > v2 */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return p1 - p2;
        }
        return 0;
    }
}
