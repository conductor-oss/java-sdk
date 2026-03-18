package patientintake.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Performs real triage assessment based on chief complaint and vitals.
 * ESI (Emergency Severity Index) levels 1-5.
 */
public class TriageWorker implements Worker {
    private static final Set<String> LEVEL1_KEYWORDS = Set.of("cardiac arrest", "not breathing", "unresponsive");
    private static final Set<String> LEVEL2_KEYWORDS = Set.of("chest pain", "stroke", "severe bleeding", "seizure", "overdose");
    private static final Set<String> LEVEL3_KEYWORDS = Set.of("abdominal pain", "difficulty breathing", "high fever", "broken bone");

    @Override public String getTaskDefName() { return "pit_triage"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String complaint = (String) task.getInputData().get("chiefComplaint");
        Object vitalsObj = task.getInputData().get("vitals");
        if (complaint == null) complaint = "";
        String lowerComplaint = complaint.toLowerCase();

        // Check vitals for abnormalities
        boolean abnormalVitals = false;
        if (vitalsObj instanceof Map) {
            Map<String, Object> vitals = (Map<String, Object>) vitalsObj;
            Object hrObj = vitals.get("hr");
            Object spo2Obj = vitals.get("spo2");
            Object tempObj = vitals.get("temp");
            if (hrObj instanceof Number && (((Number) hrObj).intValue() > 120 || ((Number) hrObj).intValue() < 50))
                abnormalVitals = true;
            if (spo2Obj instanceof Number && ((Number) spo2Obj).intValue() < 92) abnormalVitals = true;
            if (tempObj instanceof Number && ((Number) tempObj).doubleValue() > 103.0) abnormalVitals = true;
        }

        // Real triage logic
        int level;
        String dept;
        if (LEVEL1_KEYWORDS.stream().anyMatch(lowerComplaint::contains)) {
            level = 1; dept = "Resuscitation";
        } else if (LEVEL2_KEYWORDS.stream().anyMatch(lowerComplaint::contains) || abnormalVitals) {
            level = 2; dept = "Emergency";
        } else if (LEVEL3_KEYWORDS.stream().anyMatch(lowerComplaint::contains)) {
            level = 3; dept = "Urgent Care";
        } else if (!complaint.isEmpty()) {
            level = 4; dept = "Fast Track";
        } else {
            level = 3; dept = "Urgent Care"; // default
        }

        System.out.println("  [triage] \"" + complaint + "\" -> Level " + level + " (" + dept + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("triageLevel", level);
        result.getOutputData().put("department", dept);
        result.getOutputData().put("assessedAt", Instant.now().toString());
        return result;
    }
}
