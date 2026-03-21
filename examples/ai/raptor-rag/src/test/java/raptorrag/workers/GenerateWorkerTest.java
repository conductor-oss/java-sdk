package raptorrag.workers;

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
        assertEquals("rp_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithContext() {
        List<Map<String, Object>> multiLevelContext = List.of(
                Map.of("level", 2, "text", "Root overview", "relevance", 0.75),
                Map.of("level", 1, "text", "Cluster detail", "relevance", 0.88),
                Map.of("level", 0, "text", "Leaf detail", "relevance", 0.94)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "multiLevelContext", multiLevelContext
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("RAPTOR tree analysis"));
        assertTrue(answer.contains("3 levels"));
        assertTrue(answer.contains("What is Conductor?"));
        assertEquals(3, result.getOutputData().get("contextLevelsUsed"));
    }

    @Test
    void answerIncludesContextText() {
        List<Map<String, Object>> multiLevelContext = List.of(
                Map.of("level", 2, "text", "Platform overview", "relevance", 0.75),
                Map.of("level", 0, "text", "Specific details", "relevance", 0.94)
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "multiLevelContext", multiLevelContext
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Platform overview"));
        assertTrue(answer.contains("Specific details"));
        assertEquals(2, result.getOutputData().get("contextLevelsUsed"));
    }

    @Test
    void handlesNoContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("No tree context"));
        assertEquals(0, result.getOutputData().get("contextLevelsUsed"));
    }

    @Test
    void handlesEmptyContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "multiLevelContext", List.of()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No tree context"));
        assertEquals(0, result.getOutputData().get("contextLevelsUsed"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
