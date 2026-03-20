package ragmilvus.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MilvusGenerateWorkerTest {

    private final MilvusGenerateWorker worker = new MilvusGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("milvus_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithResultCount() {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(new HashMap<>(Map.of("title", "Milvus Architecture", "content", "test1")));
        results.add(new HashMap<>(Map.of("title", "Index Types", "content", "test2")));
        results.add(new HashMap<>(Map.of("title", "Partitions", "content", "test3")));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What index types does Milvus support?",
                "results", results
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Based on 3 results"));
    }

    @Test
    void answerContainsMilvusInfo() {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(new HashMap<>(Map.of("title", "test")));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "results", results
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Milvus"));
        assertTrue(answer.contains("IVF_FLAT"));
        assertTrue(answer.contains("HNSW"));
        assertTrue(answer.contains("ANNOY"));
    }

    @Test
    void handlesEmptyResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Based on 0 results"));
    }

    @Test
    void handlesEmptyQuestion() {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(new HashMap<>(Map.of("title", "test")));

        Task task = taskWith(new HashMap<>(Map.of(
                "results", results
        )));
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
