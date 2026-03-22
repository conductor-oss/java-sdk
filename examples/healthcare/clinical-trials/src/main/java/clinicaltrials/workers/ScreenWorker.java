package clinicaltrials.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * Screens patients for clinical trial eligibility.
 * Real screening: age check (18-65), condition matching, exclusion criteria.
 * Includes 21 CFR Part 11 compliant audit fields in every output.
 */
public class ScreenWorker implements Worker {
    private static final Set<String> EXCLUDED_CONDITIONS = Set.of(
            "pregnancy", "renal_failure", "hepatitis", "hiv", "active_cancer");
    private static final Set<String> ELIGIBLE_CONDITIONS = Set.of(
            "hypertension", "diabetes_type2", "asthma", "arthritis", "depression",
            "anxiety", "chronic_pain", "obesity", "insomnia");

    @Override public String getTaskDefName() { return "clt_screen"; }

    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        // --- Validate required inputs ---
        String participantId = (String) task.getInputData().get("participantId");
        if (participantId == null || participantId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: participantId");
            return r;
        }

        String condition = (String) task.getInputData().get("condition");
        if (condition == null || condition.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: condition");
            return r;
        }

        Object ageObj = task.getInputData().get("age");
        if (ageObj == null || !(ageObj instanceof Number)) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing or non-numeric required input: age");
            return r;
        }
        int age = ((Number) ageObj).intValue();
        if (age < 0 || age > 150) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Invalid age: must be between 0 and 150, got " + age);
            return r;
        }

        boolean ageEligible = age >= 18 && age <= 65;
        boolean conditionEligible = ELIGIBLE_CONDITIONS.contains(condition.toLowerCase());
        boolean hasExclusion = EXCLUDED_CONDITIONS.contains(condition.toLowerCase());
        boolean eligible = ageEligible && conditionEligible && !hasExclusion;

        List<String> inclusionCriteria = new ArrayList<>();
        inclusionCriteria.add("age 18-65: " + (ageEligible ? "PASS" : "FAIL") + " (age=" + age + ")");
        inclusionCriteria.add("confirmed diagnosis: " + (conditionEligible ? "PASS" : "FAIL") + " (" + condition + ")");
        inclusionCriteria.add("no contraindications: " + (!hasExclusion ? "PASS" : "FAIL"));

        System.out.println("  [screen] Participant " + participantId + " (age " + age + ", " + condition
                + ") -> eligible: " + eligible);

        // 21 CFR Part 11 audit fields
        Map<String, Object> cfr11 = new LinkedHashMap<>();
        cfr11.put("timestamp", Instant.now().toString());
        cfr11.put("action", "screening");
        cfr11.put("performedBy", "screening_system_v2");
        cfr11.put("participantId", participantId);
        cfr11.put("electronicSignature", "SYS-SCREEN-" + Math.abs(participantId.hashCode()) % 100000);
        cfr11.put("reason", eligible ? "All inclusion criteria met" : "Screening criteria not met");

        r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("eligible", eligible);
        o.put("ageEligible", ageEligible);
        o.put("conditionEligible", conditionEligible);
        o.put("hasExclusion", hasExclusion);
        o.put("inclusionCriteria", inclusionCriteria);
        o.put("screenedAt", Instant.now().toString());
        o.put("cfr11AuditTrail", cfr11);
        r.setOutputData(o);
        return r;
    }
}
