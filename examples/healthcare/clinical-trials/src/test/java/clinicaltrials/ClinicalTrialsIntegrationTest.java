package clinicaltrials;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import clinicaltrials.workers.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full clinical trials workflow data flow.
 * Tests: screen -> consent -> randomize -> monitor -> analyze.
 * Also tests ineligible patient path.
 */
@SuppressWarnings("unchecked")
class ClinicalTrialsIntegrationTest {

    private final ScreenWorker screenWorker = new ScreenWorker();
    private final ConsentWorker consentWorker = new ConsentWorker();
    private final RandomizeWorker randomizeWorker = new RandomizeWorker();
    private final MonitorWorker monitorWorker = new MonitorWorker();
    private final AnalyzeTrialWorker analyzeWorker = new AnalyzeTrialWorker();

    @Test
    void fullPipeline_eligiblePatient() {
        // Step 1: Screen
        TaskResult screenResult = exec(screenWorker, Map.of(
                "participantId", "SUBJ-001", "condition", "hypertension", "age", 45));
        assertEquals(TaskResult.Status.COMPLETED, screenResult.getStatus());
        boolean eligible = (boolean) screenResult.getOutputData().get("eligible");
        assertTrue(eligible, "45-year-old with hypertension should be eligible");
        assertNotNull(screenResult.getOutputData().get("cfr11AuditTrail"));

        // Step 2: Consent (only if eligible)
        TaskResult consentResult = exec(consentWorker, Map.of(
                "participantId", "SUBJ-001", "trialId", "TRIAL-2024-CARDIO-001"));
        assertEquals(TaskResult.Status.COMPLETED, consentResult.getStatus());
        assertEquals(true, consentResult.getOutputData().get("consented"));
        assertNotNull(consentResult.getOutputData().get("signatureHash"));
        assertNotNull(consentResult.getOutputData().get("cfr11AuditTrail"));

        // Step 3: Randomize
        TaskResult randomizeResult = exec(randomizeWorker, Map.of(
                "participantId", "SUBJ-001", "age", 45));
        assertEquals(TaskResult.Status.COMPLETED, randomizeResult.getStatus());
        String group = (String) randomizeResult.getOutputData().get("group");
        assertTrue("treatment".equals(group) || "control".equals(group));
        assertNotNull(randomizeResult.getOutputData().get("cfr11AuditTrail"));

        // Step 4: Monitor (uses group from randomize)
        TaskResult monitorResult = exec(monitorWorker, Map.of(
                "participantId", "SUBJ-001", "group", group));
        assertEquals(TaskResult.Status.COMPLETED, monitorResult.getStatus());
        Map<String, Object> monitoringData = (Map<String, Object>) monitorResult.getOutputData().get("monitoringData");
        assertNotNull(monitoringData);
        assertNotNull(monitoringData.get("biomarkerChange"));
        assertNotNull(monitorResult.getOutputData().get("cfr11AuditTrail"));

        // Step 5: Analyze (uses monitoring data)
        TaskResult analyzeResult = exec(analyzeWorker, Map.of("monitoringData", monitoringData));
        assertEquals(TaskResult.Status.COMPLETED, analyzeResult.getStatus());
        assertNotNull(analyzeResult.getOutputData().get("outcome"));
        assertNotNull(analyzeResult.getOutputData().get("pValue"));
        assertNotNull(analyzeResult.getOutputData().get("cfr11AuditTrail"));
    }

    @Test
    void ineligiblePatient_tooYoung() {
        TaskResult screenResult = exec(screenWorker, Map.of(
                "participantId", "SUBJ-MINOR", "condition", "hypertension", "age", 15));
        assertEquals(TaskResult.Status.COMPLETED, screenResult.getStatus());
        assertEquals(false, screenResult.getOutputData().get("eligible"));
        assertEquals(false, screenResult.getOutputData().get("ageEligible"));
        // Pipeline stops here -- no consent, randomize, etc. for ineligible
    }

    @Test
    void ineligiblePatient_excludedCondition() {
        TaskResult screenResult = exec(screenWorker, Map.of(
                "participantId", "SUBJ-EXCLUDED", "condition", "pregnancy", "age", 30));
        assertEquals(TaskResult.Status.COMPLETED, screenResult.getStatus());
        assertEquals(false, screenResult.getOutputData().get("eligible"));
        assertEquals(true, screenResult.getOutputData().get("hasExclusion"));
    }

    @Test
    void ineligiblePatient_nonMatchingCondition() {
        TaskResult screenResult = exec(screenWorker, Map.of(
                "participantId", "SUBJ-WRONG-COND", "condition", "broken_arm", "age", 30));
        assertEquals(false, screenResult.getOutputData().get("eligible"));
        assertEquals(false, screenResult.getOutputData().get("conditionEligible"));
    }

    @Test
    void cfr11AuditTrailPresentAtEveryStep() {
        // Screen
        TaskResult r1 = exec(screenWorker, Map.of(
                "participantId", "SUBJ-AUDIT", "condition", "hypertension", "age", 40));
        Map<String, Object> a1 = (Map<String, Object>) r1.getOutputData().get("cfr11AuditTrail");
        assertEquals("screening", a1.get("action"));
        assertNotNull(a1.get("electronicSignature"));

        // Consent
        TaskResult r2 = exec(consentWorker, Map.of(
                "participantId", "SUBJ-AUDIT", "trialId", "TRIAL-AUDIT"));
        Map<String, Object> a2 = (Map<String, Object>) r2.getOutputData().get("cfr11AuditTrail");
        assertEquals("informed_consent", a2.get("action"));
        assertNotNull(a2.get("electronicSignature"));

        // Randomize
        TaskResult r3 = exec(randomizeWorker, Map.of("participantId", "SUBJ-AUDIT", "age", 40));
        Map<String, Object> a3 = (Map<String, Object>) r3.getOutputData().get("cfr11AuditTrail");
        assertEquals("randomization", a3.get("action"));

        // Monitor
        String group = (String) r3.getOutputData().get("group");
        TaskResult r4 = exec(monitorWorker, Map.of("participantId", "SUBJ-AUDIT", "group", group));
        Map<String, Object> a4 = (Map<String, Object>) r4.getOutputData().get("cfr11AuditTrail");
        assertEquals("monitoring", a4.get("action"));

        // Analyze
        Map<String, Object> monData = (Map<String, Object>) r4.getOutputData().get("monitoringData");
        TaskResult r5 = exec(analyzeWorker, Map.of("monitoringData", monData));
        Map<String, Object> a5 = (Map<String, Object>) r5.getOutputData().get("cfr11AuditTrail");
        assertEquals("trial_analysis", a5.get("action"));
    }

    @Test
    void invalidInput_failsAtFirstStep() {
        // Missing age should fail screening with terminal error
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "SUBJ-BAD", "condition", "hypertension")));
        TaskResult r = screenWorker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    private TaskResult exec(com.netflix.conductor.client.worker.Worker worker, Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return worker.execute(task);
    }
}
