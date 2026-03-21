package sendgridintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersonalizeWorkerTest {

    private final PersonalizeWorker worker = new PersonalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("sgd_personalize", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("recipientName", "Alice", "recipientEmail", "alice@example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Welcome to our platform, Alice!", result.getOutputData().get("subject"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
