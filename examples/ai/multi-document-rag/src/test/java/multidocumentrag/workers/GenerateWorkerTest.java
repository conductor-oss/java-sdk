package multidocumentrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("mdrag_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromMergedContext() {
        List<Map<String, Object>> mergedContext = new ArrayList<>(List.of(
                new HashMap<>(Map.of("text", "result1", "source", "api_docs", "score", 0.95)),
                new HashMap<>(Map.of("text", "result2", "source", "tutorials", "score", 0.92)),
                new HashMap<>(Map.of("text", "result3", "source", "forums", "score", 0.90)),
                new HashMap<>(Map.of("text", "result4", "source", "api_docs", "score", 0.88)),
                new HashMap<>(Map.of("text", "result5", "source", "tutorials", "score", 0.85))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do I create and run a Conductor workflow?",
                "mergedContext", mergedContext)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("5 sources"));
        assertTrue(answer.contains("API docs"));
        assertTrue(answer.contains("tutorials"));
        assertTrue(answer.contains("forums"));
    }

    @Test
    void handlesNullMergedContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 sources"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
