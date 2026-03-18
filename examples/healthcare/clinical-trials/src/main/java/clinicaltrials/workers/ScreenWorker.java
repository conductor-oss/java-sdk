package clinicaltrials.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * Screens patients for clinical trial eligibility.
 * Real screening: age check (18-65), condition matching, exclusion criteria.
 */
public class ScreenWorker implements Worker {
    private static final Set<String> EXCLUDED_CONDITIONS = Set.of(
            "pregnancy", "renal_failure", "hepatitis", "hiv", "active_cancer");
    private static final Set<String> ELIGIBLE_CONDITIONS = Set.of(
            "hypertension", "diabetes_type2", "asthma", "arthritis", "depression",
            "anxiety", "chronic_pain", "obesity", "insomnia");

    @Override public String getTaskDefName() { return "clt_screen"; }

    @Override public TaskResult execute(Task task) {
        String participantId = (String) task.getInputData().get("participantId");
        String condition = (String) task.getInputData().get("condition");
        Object ageObj = task.getInputData().get("age");
        if (participantId == null) participantId = "UNKNOWN";
        if (condition == null) condition = "";

        int age = 35; // default
        if (ageObj instanceof Number) age = ((Number) ageObj).intValue();

        boolean ageEligible = age >= 18 && age <= 65;
        boolean conditionEligible = ELIGIBLE_CONDITIONS.contains(condition.toLowerCase());
        boolean hasExclusion = EXCLUDED_CONDITIONS.contains(condition.toLowerCase());
        boolean eligible = ageEligible && (conditionEligible || condition.isEmpty()) && !hasExclusion;

        List<String> inclusionCriteria = new ArrayList<>();
        inclusionCriteria.add("age 18-65: " + (ageEligible ? "PASS" : "FAIL") + " (age=" + age + ")");
        inclusionCriteria.add("confirmed diagnosis: " + (conditionEligible ? "PASS" : (condition.isEmpty() ? "N/A" : "FAIL")));
        inclusionCriteria.add("no contraindications: " + (!hasExclusion ? "PASS" : "FAIL"));

        System.out.println("  [screen] Participant " + participantId + " (age " + age + ", " + condition
                + ") -> eligible: " + eligible);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("eligible", eligible);
        o.put("ageEligible", ageEligible);
        o.put("conditionEligible", conditionEligible);
        o.put("hasExclusion", hasExclusion);
        o.put("inclusionCriteria", inclusionCriteria);
        o.put("screenedAt", Instant.now().toString());
        r.setOutputData(o);
        return r;
    }
}
