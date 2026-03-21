package kycaml.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecideWorkerTest {

    private final DecideWorker worker = new DecideWorker();

    @Test
    void taskDefName() {
        assertEquals("kyc_decide", worker.getTaskDefName());
    }

    @Test
    void approvesLowRisk() {
        Task task = taskWith(Map.of("riskLevel", "low", "riskScore", 15));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("decision"));
        assertEquals(false, result.getOutputData().get("reviewRequired"));
    }

    @Test
    void approvesWithMonitoringForMediumRisk() {
        Task task = taskWith(Map.of("riskLevel", "medium", "riskScore", 45));
        TaskResult result = worker.execute(task);

        assertEquals("approved_with_monitoring", result.getOutputData().get("decision"));
        assertEquals(false, result.getOutputData().get("reviewRequired"));
    }

    @Test
    void enhancedDueDiligenceForHighRisk() {
        Task task = taskWith(Map.of("riskLevel", "high", "riskScore", 85));
        TaskResult result = worker.execute(task);

        assertEquals("enhanced_due_diligence", result.getOutputData().get("decision"));
        assertEquals(true, result.getOutputData().get("reviewRequired"));
    }

    @Test
    void defaultsToMediumWhenRiskLevelNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("riskLevel", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("approved_with_monitoring", result.getOutputData().get("decision"));
    }

    @Test
    void includesTimestamp() {
        Task task = taskWith(Map.of("riskLevel", "low", "riskScore", 10));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("decidedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
