package rollingupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeTest {

    private final Analyze worker = new Analyze();

    @Test
    void taskDefName() {
        assertEquals("ru_analyze", worker.getTaskDefName());
    }

    @Test
    void userServiceHasFiveReplicas() {
        Task task = taskWith("user-service", "2.5.0");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("currentReplicas"));
        assertEquals("2.4.0", result.getOutputData().get("currentVersion"));
    }

    @Test
    void orderServiceHasThreeReplicas() {
        Task task = taskWith("order-service", "2.0.0");
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("currentReplicas"));
        assertEquals("1.8.2", result.getOutputData().get("currentVersion"));
    }

    @Test
    void paymentServiceHasFourReplicas() {
        Task task = taskWith("payment-service", "4.0.0");
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("currentReplicas"));
    }

    @Test
    void unknownServiceDefaultsToTwoReplicas() {
        Task task = taskWith("mystery-service", "1.0.0");
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("currentReplicas"));
        assertEquals("1.0.0", result.getOutputData().get("currentVersion"));
    }

    @Test
    void outputContainsAnalyzeId() {
        Task task = taskWith("user-service", "2.5.0");
        TaskResult result = worker.execute(task);

        assertEquals("ANALYZE-1342", result.getOutputData().get("analyzeId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void allReplicasAreHealthy() {
        Task task = taskWith("user-service", "2.5.0");
        TaskResult result = worker.execute(task);

        assertEquals(result.getOutputData().get("currentReplicas"),
                     result.getOutputData().get("healthyReplicas"));
    }

    @Test
    void newVersionIsPassedThrough() {
        Task task = taskWith("user-service", "3.0.0-beta");
        TaskResult result = worker.execute(task);

        assertEquals("3.0.0-beta", result.getOutputData().get("newVersion"));
    }

    @Test
    void nullInputsDefaultGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-service", result.getOutputData().get("service"));
        assertEquals("0.0.0", result.getOutputData().get("newVersion"));
    }

    private Task taskWith(String service, String newVersion) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("service", service);
        input.put("newVersion", newVersion);
        task.setInputData(input);
        return task;
    }
}
