package securitytraining.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StAssignTrainingWorkerTest {

    private final StAssignTrainingWorker worker = new StAssignTrainingWorker();

    @Test
    void taskDefName() {
        assertEquals("st_assign_training", worker.getTaskDefName());
    }

    @Test
    void completesWithTrainingId() {
        Task task = taskWith(Map.of("department", "engineering", "trainingModule", "secure-coding-2024"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ASSIGN_TRAINING-1393", result.getOutputData().get("assign_trainingId"));
    }

    @Test
    void outputContainsSuccess() {
        Task task = taskWith(Map.of("department", "hr", "trainingModule", "phishing-awareness"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullDepartment() {
        Map<String, Object> input = new HashMap<>();
        input.put("department", null);
        input.put("trainingModule", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullTrainingModule() {
        Map<String, Object> input = new HashMap<>();
        input.put("department", "eng");
        input.put("trainingModule", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("assign_trainingId"));
    }

    @Test
    void outputIsDeterministic() {
        Task task = taskWith(Map.of("department", "eng", "trainingModule", "m1"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("assign_trainingId"), r2.getOutputData().get("assign_trainingId"));
    }

    @Test
    void outputHasTwoEntries() {
        Task task = taskWith(Map.of("department", "eng", "trainingModule", "m1"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
