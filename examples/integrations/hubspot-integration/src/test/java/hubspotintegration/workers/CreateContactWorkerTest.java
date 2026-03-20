package hubspotintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateContactWorkerTest {

    private final CreateContactWorker worker = new CreateContactWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_create_contact", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("email", "bob@test.com", "firstName", "Bob", "lastName", "Smith", "company", "TestCorp"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("contactId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
