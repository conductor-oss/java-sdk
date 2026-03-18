package apicalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatOutputWorkerTest {

    private final FormatOutputWorker worker = new FormatOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("ap_format_output", worker.getTaskDefName());
    }

    @Test
    void returnsFormattedAnswer() {
        Map<String, Object> parsedData = Map.of(
                "name", "conductor-oss/conductor",
                "description", "an orchestration platform",
                "stars", 16500,
                "forks", 2100,
                "language", "Java",
                "license", "Apache License 2.0"
        );
        Task task = taskWith(Map.of(
                "userRequest", "Tell me about Conductor",
                "parsedData", parsedData,
                "apiName", "github"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("conductor-oss/conductor"));
        assertTrue(answer.contains("16500"));
        assertTrue(answer.contains("Java"));
    }

    @Test
    void returnsDataSource() {
        Map<String, Object> parsedData = Map.of("name", "test/repo");
        Task task = taskWith(Map.of(
                "userRequest", "Get info",
                "parsedData", parsedData,
                "apiName", "github"));
        TaskResult result = worker.execute(task);

        assertEquals("github_api", result.getOutputData().get("dataSource"));
    }

    @Test
    void returnsFieldsUsedCount() {
        Map<String, Object> parsedData = Map.of(
                "name", "test/repo",
                "description", "A test",
                "stars", 100,
                "forks", 10
        );
        Task task = taskWith(Map.of(
                "userRequest", "Show repo",
                "parsedData", parsedData,
                "apiName", "github"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("fieldsUsed"));
    }

    @Test
    void handlesNullParsedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", "Tell me about Conductor");
        input.put("parsedData", null);
        input.put("apiName", "github");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No data"));
        assertEquals(0, result.getOutputData().get("fieldsUsed"));
    }

    @Test
    void handlesEmptyParsedData() {
        Task task = taskWith(Map.of(
                "userRequest", "Tell me about Conductor",
                "parsedData", Map.of(),
                "apiName", "github"));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No data"));
        assertEquals(0, result.getOutputData().get("fieldsUsed"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        input.put("parsedData", Map.of("name", "test/repo"));
        input.put("apiName", "github");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullApiName() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", "Get info");
        input.put("parsedData", Map.of("name", "test/repo"));
        input.put("apiName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown_api", result.getOutputData().get("dataSource"));
    }

    @Test
    void answerIncludesLicenseInfo() {
        Map<String, Object> parsedData = Map.of(
                "name", "test/repo",
                "description", "A project",
                "stars", 500,
                "forks", 50,
                "language", "Go",
                "license", "MIT License"
        );
        Task task = taskWith(Map.of(
                "userRequest", "Repo info",
                "parsedData", parsedData,
                "apiName", "github"));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("MIT License"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
