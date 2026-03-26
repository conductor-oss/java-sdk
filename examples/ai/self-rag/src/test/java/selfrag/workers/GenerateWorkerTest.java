package selfrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_generate", worker.getTaskDefName());
    }

    @Test
    void returnsAnswerAboutConductorTaskTypes() {
        List<Map<String, Object>> relevantDocs = List.of(
                Map.of("id", "d1", "text", "Conductor tasks can be SIMPLE or SYSTEM.", "score", 0.91)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What task types does Conductor support?",
                "relevantDocs", relevantDocs)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("SIMPLE"));
        assertTrue(answer.contains("FORK_JOIN"));
        assertTrue(answer.contains("DO_WHILE"));
    }

    @Test
    void outputContainsAnswerField() {
        List<Map<String, Object>> relevantDocs = List.of(
                Map.of("id", "d1", "text", "doc", "score", 0.9)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "relevantDocs", relevantDocs)));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
