package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class AnalyzeTrialWorkerTest {
    private final AnalyzeTrialWorker w = new AnalyzeTrialWorker();

    @Test void taskDefName() { assertEquals("clt_analyze", w.getTaskDefName()); }

    @Test void detectsImprovement() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -12.5, "compliance", 95, "adverseEvents", 0)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("improvement", r.getOutputData().get("outcome"));
        assertNotNull(r.getOutputData().get("cfr11AuditTrail"));
    }

    @Test void noChangeForSmallEffect() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", 1.0, "compliance", 90, "adverseEvents", 0)));
        assertEquals("no_change", r.getOutputData().get("outcome"));
    }

    @Test void detectsDeterioration() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", 10.0, "compliance", 90, "adverseEvents", 0)));
        assertEquals("deterioration", r.getOutputData().get("outcome"));
    }

    @Test void safetyNotAcceptableWithHighAdverseEvents() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -10.0, "compliance", 90, "adverseEvents", 5)));
        assertEquals(false, r.getOutputData().get("safetyAcceptable"));
    }

    @Test void safetyNotAcceptableWithLowCompliance() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -10.0, "compliance", 50, "adverseEvents", 0)));
        assertEquals(false, r.getOutputData().get("safetyAcceptable"));
    }

    @Test void cfr11AuditTrailContainsRequiredFields() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -12.5, "compliance", 95, "adverseEvents", 0)));
        Map<String, Object> cfr11 = (Map<String, Object>) r.getOutputData().get("cfr11AuditTrail");
        assertNotNull(cfr11);
        assertNotNull(cfr11.get("timestamp"));
        assertEquals("trial_analysis", cfr11.get("action"));
        assertNotNull(cfr11.get("performedBy"));
        assertNotNull(cfr11.get("electronicSignature"));
        assertNotNull(cfr11.get("outcome"));
        assertNotNull(cfr11.get("pValue"));
    }

    // ---- Failure path ---------------------------------------------------

    @Test void failsWithTerminalErrorOnMissingMonitoringData() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("monitoringData"));
    }

    @Test void failsWithTerminalErrorOnMissingBiomarkerChange() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "compliance", 90, "adverseEvents", 0)));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("biomarkerChange"));
    }

    @Test void failsWithTerminalErrorOnMissingCompliance() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -5.0, "adverseEvents", 0)));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("compliance"));
    }

    @Test void failsWithTerminalErrorOnMissingAdverseEvents() {
        TaskResult r = exec(Map.of("monitoringData", Map.of(
                "biomarkerChange", -5.0, "compliance", 90)));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("adverseEvents"));
    }

    private TaskResult exec(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(input));
        return w.execute(t);
    }
}
