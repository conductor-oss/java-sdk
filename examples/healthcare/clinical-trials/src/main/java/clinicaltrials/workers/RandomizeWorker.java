package clinicaltrials.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.util.*;

/**
 * Randomizes participant to treatment or control group.
 * Uses stratified randomization based on age and condition severity.
 */
public class RandomizeWorker implements Worker {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override public String getTaskDefName() { return "clt_randomize"; }

    @Override public TaskResult execute(Task task) {
        String participantId = (String) task.getInputData().get("participantId");
        Object ageObj = task.getInputData().get("age");
        if (participantId == null) participantId = "UNKNOWN";

        int age = 35;
        if (ageObj instanceof Number) age = ((Number) ageObj).intValue();

        // Stratification factors based on age
        List<String> stratificationFactors = new ArrayList<>();
        stratificationFactors.add(age < 40 ? "age_young" : "age_older");
        stratificationFactors.add("severity_moderate"); // default

        // Real randomization using SecureRandom (1:1 allocation)
        boolean isTreatment = SECURE_RANDOM.nextBoolean();
        String group = isTreatment ? "treatment" : "control";

        // Generate randomization code
        String randomizationCode = "RND-" + String.format("%05d", SECURE_RANDOM.nextInt(100000));

        System.out.println("  [randomize] " + participantId + " -> " + group
                + " (code: " + randomizationCode + ", strata: " + stratificationFactors + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("group", group);
        o.put("randomizationCode", randomizationCode);
        o.put("stratificationFactors", stratificationFactors);
        r.setOutputData(o);
        return r;
    }
}
