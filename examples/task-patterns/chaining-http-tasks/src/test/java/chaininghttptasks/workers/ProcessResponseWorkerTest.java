package chaininghttptasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessResponseWorkerTest {

    private final ProcessResponseWorker worker = new ProcessResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("http_process_response", worker.getTaskDefName());
    }

    @Test
    void processesResponseWithResults() {
        Map<String, Object> responseBody = Map.of(
                "results", List.of(
                        Map.of("workflowId", "abc123"),
                        Map.of("workflowId", "def456")
                )
        );
        Task task = taskWith(Map.of(
                "statusCode", 200,
                "responseBody", responseBody
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Search returned 2 results", result.getOutputData().get("result"));
        assertEquals(200, result.getOutputData().get("httpStatus"));
    }

    @Test
    void processesResponseWithEmptyResults() {
        Map<String, Object> responseBody = Map.of("results", List.of());
        Task task = taskWith(Map.of(
                "statusCode", 200,
                "responseBody", responseBody
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Search returned 0 results", result.getOutputData().get("result"));
    }

    @Test
    void processesResponseWithNoBody() {
        Task task = taskWith(Map.of("statusCode", 204));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Search returned 0 results", result.getOutputData().get("result"));
        assertEquals(204, result.getOutputData().get("httpStatus"));
    }

    @Test
    void handlesNullStatusCode() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("httpStatus"));
    }

    @Test
    void processesResponseWithMissingResultsKey() {
        Map<String, Object> responseBody = Map.of("totalHits", 0);
        Task task = taskWith(Map.of(
                "statusCode", 200,
                "responseBody", responseBody
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Search returned 0 results", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
