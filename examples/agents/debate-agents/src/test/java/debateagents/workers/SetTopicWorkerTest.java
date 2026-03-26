package debateagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetTopicWorkerTest {

    private final SetTopicWorker worker = new SetTopicWorker();

    @Test
    void taskDefName() {
        assertEquals("da_set_topic", worker.getTaskDefName());
    }

    @Test
    void returnsTopicAndSides() {
        Task task = taskWith(Map.of("topic", "Microservices vs Monolithic Architecture"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Microservices vs Monolithic Architecture", result.getOutputData().get("topic"));

        @SuppressWarnings("unchecked")
        List<String> sides = (List<String>) result.getOutputData().get("sides");
        assertNotNull(sides);
        assertEquals(2, sides.size());
        assertEquals("PRO", sides.get(0));
        assertEquals("CON", sides.get(1));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Technology in modern society", result.getOutputData().get("topic"));
        assertNotNull(result.getOutputData().get("sides"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Technology in modern society", result.getOutputData().get("topic"));
    }

    @Test
    void handlesBlankTopic() {
        Task task = taskWith(Map.of("topic", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Technology in modern society", result.getOutputData().get("topic"));
    }

    @Test
    void preservesExactTopicString() {
        String topic = "  AI Ethics in Healthcare  ";
        Task task = taskWith(Map.of("topic", topic));
        TaskResult result = worker.execute(task);

        assertEquals(topic, result.getOutputData().get("topic"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
