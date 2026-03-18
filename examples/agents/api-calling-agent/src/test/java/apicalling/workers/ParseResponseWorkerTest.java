package apicalling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseResponseWorkerTest {

    private final ParseResponseWorker worker = new ParseResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("ap_parse_response", worker.getTaskDefName());
    }

    @Test
    void parsesMappedFields() {
        Map<String, Object> rawResponse = Map.of(
                "full_name", "conductor-oss/conductor",
                "description", "An orchestration platform",
                "stargazers_count", 16500,
                "forks_count", 2100,
                "language", "Java",
                "license", Map.of("key", "apache-2.0", "name", "Apache License 2.0"),
                "open_issues_count", 42,
                "default_branch", "main"
        );
        Task task = taskWith(Map.of("rawResponse", rawResponse, "statusCode", 200));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertEquals("conductor-oss/conductor", parsed.get("name"));
        assertEquals(16500, parsed.get("stars"));
        assertEquals(2100, parsed.get("forks"));
        assertEquals("Java", parsed.get("language"));
        assertEquals("Apache License 2.0", parsed.get("license"));
    }

    @Test
    void fieldsExtractedCount() {
        Map<String, Object> rawResponse = Map.of(
                "full_name", "test/repo",
                "description", "A test repo",
                "stargazers_count", 100,
                "forks_count", 10,
                "language", "Python",
                "license", Map.of("name", "MIT License"),
                "open_issues_count", 5,
                "default_branch", "main"
        );
        Task task = taskWith(Map.of("rawResponse", rawResponse, "statusCode", 200));
        TaskResult result = worker.execute(task);

        assertEquals(8, result.getOutputData().get("fieldsExtracted"));
    }

    @Test
    void validationPassedWhenStatus200() {
        Map<String, Object> rawResponse = Map.of("full_name", "test/repo");
        Task task = taskWith(Map.of("rawResponse", rawResponse, "statusCode", 200));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("validationPassed"));
    }

    @Test
    void validationFailsWhenStatusNot200() {
        Map<String, Object> rawResponse = Map.of("full_name", "test/repo");
        Task task = taskWith(Map.of("rawResponse", rawResponse, "statusCode", 404));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("validationPassed"));
    }

    @Test
    void validationFailsWhenNoFields() {
        Task task = taskWith(Map.of("statusCode", 200));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("validationPassed"));
        assertEquals(0, result.getOutputData().get("fieldsExtracted"));
    }

    @Test
    void handlesNullRawResponse() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawResponse", null);
        input.put("statusCode", 200);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("fieldsExtracted"));
    }

    @Test
    void handlesMissingStatusCode() {
        Map<String, Object> rawResponse = Map.of("full_name", "test/repo");
        Task task = taskWith(Map.of("rawResponse", rawResponse));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("validationPassed"));
    }

    @Test
    void parsesLicenseAsString() {
        Map<String, Object> rawResponse = Map.of(
                "full_name", "test/repo",
                "license", "MIT"
        );
        Task task = taskWith(Map.of("rawResponse", rawResponse, "statusCode", 200));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertEquals("MIT", parsed.get("license"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
