package semistructuredrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyDataWorkerTest {

    private final ClassifyDataWorker worker = new ClassifyDataWorker();

    @Test
    void taskDefName() {
        assertEquals("ss_classify_data", worker.getTaskDefName());
    }

    @Test
    void returnsFourStructuredFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is the revenue?",
                "dataContext", "financial data"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fields = (List<Map<String, String>>) result.getOutputData().get("structuredFields");
        assertNotNull(fields);
        assertEquals(4, fields.size());
    }

    @Test
    void structuredFieldsHaveRequiredKeys() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "dataContext", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> fields = (List<Map<String, String>>) result.getOutputData().get("structuredFields");

        for (Map<String, String> field : fields) {
            assertNotNull(field.get("field"));
            assertNotNull(field.get("type"));
            assertNotNull(field.get("source"));
        }
    }

    @Test
    void returnsThreeTextChunks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "dataContext", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> chunks = (List<Map<String, String>>) result.getOutputData().get("textChunks");
        assertNotNull(chunks);
        assertEquals(3, chunks.size());
    }

    @Test
    void textChunksHaveRequiredKeys() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "dataContext", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> chunks = (List<Map<String, String>>) result.getOutputData().get("textChunks");

        for (Map<String, String> chunk : chunks) {
            assertNotNull(chunk.get("id"));
            assertNotNull(chunk.get("type"));
            assertNotNull(chunk.get("content"));
        }
    }

    @Test
    void returnsDataTypesList() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "dataContext", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> dataTypes = (List<String>) result.getOutputData().get("dataTypes");
        assertNotNull(dataTypes);
        assertFalse(dataTypes.isEmpty());
        assertTrue(dataTypes.contains("numeric"));
        assertTrue(dataTypes.contains("report"));
    }

    @Test
    void handlesNullInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("structuredFields"));
        assertNotNull(result.getOutputData().get("textChunks"));
        assertNotNull(result.getOutputData().get("dataTypes"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
