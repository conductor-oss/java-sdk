package ragweaviate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeavSearchWorkerTest {

    private final WeavSearchWorker worker = new WeavSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("weav_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeObjects() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0123, -0.0456, 0.0789, -0.0321, 0.0654),
                "className", "Document",
                "properties", List.of("title", "content", "source"),
                "limit", 3
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objects =
                (List<Map<String, Object>>) result.getOutputData().get("objects");
        assertNotNull(objects);
        assertEquals(3, objects.size());
    }

    @Test
    void firstObjectHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0123, -0.0456, 0.0789),
                "className", "Document",
                "properties", List.of("title", "content"),
                "limit", 3
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objects =
                (List<Map<String, Object>>) result.getOutputData().get("objects");

        Map<String, Object> first = objects.get(0);
        assertEquals("Weaviate Overview", first.get("title"));
        assertEquals("Weaviate is an open-source vector database with built-in vectorization modules.",
                first.get("content"));
        assertEquals("docs/intro.md", first.get("source"));

        @SuppressWarnings("unchecked")
        Map<String, Object> additional = (Map<String, Object>) first.get("_additional");
        assertEquals(0.95, additional.get("certainty"));
        assertEquals(0.05, additional.get("distance"));
    }

    @Test
    void secondObjectHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0),
                "className", "Document",
                "properties", List.of("title"),
                "limit", 3
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objects =
                (List<Map<String, Object>>) result.getOutputData().get("objects");

        Map<String, Object> second = objects.get(1);
        assertEquals("Schema Design", second.get("title"));
        assertEquals("Classes in Weaviate define the data structure with properties and vectorizer config.",
                second.get("content"));
        assertEquals("docs/schema.md", second.get("source"));

        @SuppressWarnings("unchecked")
        Map<String, Object> additional = (Map<String, Object>) second.get("_additional");
        assertEquals(0.91, additional.get("certainty"));
        assertEquals(0.09, additional.get("distance"));
    }

    @Test
    void thirdObjectHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0),
                "className", "Document",
                "properties", List.of("title"),
                "limit", 3
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objects =
                (List<Map<String, Object>>) result.getOutputData().get("objects");

        Map<String, Object> third = objects.get(2);
        assertEquals("Modules", third.get("title"));
        assertEquals("Weaviate supports text2vec-openai, text2vec-huggingface, and generative-openai modules.",
                third.get("content"));
        assertEquals("docs/modules.md", third.get("source"));

        @SuppressWarnings("unchecked")
        Map<String, Object> additional = (Map<String, Object>) third.get("_additional");
        assertEquals(0.88, additional.get("certainty"));
        assertEquals(0.12, additional.get("distance"));
    }

    @Test
    void queryInfoContainsExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0),
                "className", "Article",
                "properties", List.of("title"),
                "limit", 5
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> queryInfo =
                (Map<String, Object>) result.getOutputData().get("queryInfo");
        assertNotNull(queryInfo);
        assertEquals("Article", queryInfo.get("className"));
        assertEquals("text2vec-openai", queryInfo.get("vectorizer"));
        assertEquals(8432, queryInfo.get("totalObjects"));
    }

    @Test
    void usesDefaultClassNameWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.0),
                "properties", List.of("title"),
                "limit", 3
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> queryInfo =
                (Map<String, Object>) result.getOutputData().get("queryInfo");
        assertEquals("Document", queryInfo.get("className"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
