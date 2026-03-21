package apikeyrotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateNewWorkerTest {

    private final GenerateNewWorker worker = new GenerateNewWorker();

    @Test
    void taskDefName() {
        assertEquals("akr_generate_new", worker.getTaskDefName());
    }

    @Test
    void generatesKeyForValidService() {
        Task task = taskWith("payment-api");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
        assertNotNull(result.getOutputData().get("generate_newId"));
    }

    @Test
    void defaultsServiceToUnknown() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsGenerateNewId() {
        Task task = taskWith("auth-service");

        TaskResult result = worker.execute(task);

        String id = (String) result.getOutputData().get("generate_newId");
        assertNotNull(id);
        assertTrue(id.startsWith("GENERATE_NEW-"));
    }

    private Task taskWith(String service) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("service", service);
        task.setInputData(input);
        return task;
    }
}
