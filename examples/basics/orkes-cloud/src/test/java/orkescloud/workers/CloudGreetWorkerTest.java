package orkescloud.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudGreetWorkerTest {

    private final CloudGreetWorker cloudWorker = new CloudGreetWorker(true);
    private final CloudGreetWorker localWorker = new CloudGreetWorker(false);

    @Test
    void cloudTaskDefName() {
        assertEquals("cloud_greet", cloudWorker.getTaskDefName());
    }

    @Test
    void localTaskDefName() {
        assertEquals("local_greet", localWorker.getTaskDefName());
    }

    @Test
    void cloudGreetsWithCloudIndicator() {
        Task task = taskWith(Map.of("name", "Alice"));
        TaskResult result = cloudWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String greeting = (String) result.getOutputData().get("greeting");
        assertTrue(greeting.contains("Alice"));
        assertTrue(greeting.contains("Cloud"));
        assertEquals("Cloud", result.getOutputData().get("mode"));
    }

    @Test
    void localGreetsWithLocalIndicator() {
        Task task = taskWith(Map.of("name", "Bob"));
        TaskResult result = localWorker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String greeting = (String) result.getOutputData().get("greeting");
        assertTrue(greeting.contains("Bob"));
        assertTrue(greeting.contains("Local"));
        assertEquals("Local", result.getOutputData().get("mode"));
    }

    @Test
    void defaultsToWorldWhenNameMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = cloudWorker.execute(task);

        String greeting = (String) result.getOutputData().get("greeting");
        assertTrue(greeting.contains("World"));
    }

    @Test
    void defaultsToWorldWhenNameBlank() {
        Task task = taskWith(Map.of("name", "   "));
        TaskResult result = localWorker.execute(task);

        String greeting = (String) result.getOutputData().get("greeting");
        assertTrue(greeting.contains("World"));
    }

    @Test
    void outputContainsGreetingAndMode() {
        Task task = taskWith(Map.of("name", "Test"));
        TaskResult result = cloudWorker.execute(task);

        assertNotNull(result.getOutputData().get("greeting"));
        assertNotNull(result.getOutputData().get("mode"));
    }

    @Test
    void greetingFormat() {
        Task task = taskWith(Map.of("name", "Developer"));
        TaskResult result = cloudWorker.execute(task);

        assertEquals("Hello, Developer! Connected via Orkes Cloud mode.",
                result.getOutputData().get("greeting"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
