package riskassessment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectFactorsWorkerTest {

    private final CollectFactorsWorker worker = new CollectFactorsWorker();

    @Test
    void taskDefName() {
        assertEquals("rsk_collect_factors", worker.getTaskDefName());
    }

    @Test
    void collectsAllRiskFactors() {
        Task task = taskWith(Map.of("portfolioId", "PORT-1", "assessmentDate", "2024-01-01"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("marketData"));
        assertNotNull(result.getOutputData().get("creditData"));
        assertNotNull(result.getOutputData().get("operationalData"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
