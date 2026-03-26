package ragweaviate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeavGenerateWorkerTest {

    private final WeavGenerateWorker worker = new WeavGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("weav_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromQuestionAndObjects() {
        Map<String, Object> obj1 = new LinkedHashMap<>();
        obj1.put("title", "Weaviate Overview");
        obj1.put("content", "Weaviate is an open-source vector database.");

        Map<String, Object> obj2 = new LinkedHashMap<>();
        obj2.put("title", "Schema Design");
        obj2.put("content", "Classes define the data structure.");

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Weaviate?",
                "objects", List.of(obj1, obj2)
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("2 retrieved documents"));
        assertTrue(answer.contains("What is Weaviate?"));
        assertTrue(answer.contains("Weaviate Overview"));
        assertTrue(answer.contains("Schema Design"));
    }

    @Test
    void handlesEmptyObjectsList() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Test question"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("0 retrieved documents"));
    }

    @Test
    void answerIncludesAllObjectTitles() {
        Map<String, Object> obj1 = new LinkedHashMap<>();
        obj1.put("title", "Title A");
        obj1.put("content", "Content A");

        Map<String, Object> obj2 = new LinkedHashMap<>();
        obj2.put("title", "Title B");
        obj2.put("content", "Content B");

        Map<String, Object> obj3 = new LinkedHashMap<>();
        obj3.put("title", "Title C");
        obj3.put("content", "Content C");

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Tell me everything",
                "objects", List.of(obj1, obj2, obj3)
        )));

        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("3 retrieved documents"));
        assertTrue(answer.contains("Title A"));
        assertTrue(answer.contains("Title B"));
        assertTrue(answer.contains("Title C"));
    }

    @Test
    void outputContainsAnswerKey() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "objects", List.of()
        )));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
