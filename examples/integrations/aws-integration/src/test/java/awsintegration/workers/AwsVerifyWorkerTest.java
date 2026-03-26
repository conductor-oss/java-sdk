package awsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AwsVerifyWorkerTest {

    private final AwsVerifyWorker worker = new AwsVerifyWorker();

    @Test
    void taskDefName() {
        assertEquals("aws_verify", worker.getTaskDefName());
    }

    @Test
    void verifiesAllServicesPresent() {
        Task task = taskWith(Map.of(
                "s3Result", "data/evt-5001.json",
                "dynamoResult", "evt-5001",
                "snsResult", "sns-msg-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void failsVerificationWhenS3Missing() {
        Map<String, Object> input = new HashMap<>();
        input.put("s3Result", null);
        input.put("dynamoResult", "evt-5001");
        input.put("snsResult", "sns-msg-001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsVerificationWhenDynamoMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("s3Result", "data/evt-5001.json");
        input.put("dynamoResult", null);
        input.put("snsResult", "sns-msg-001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsVerificationWhenSnsMissing() {
        Map<String, Object> input = new HashMap<>();
        input.put("s3Result", "data/evt-5001.json");
        input.put("dynamoResult", "evt-5001");
        input.put("snsResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsVerificationWhenAllMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void failsVerificationWithEmptyStrings() {
        Task task = taskWith(Map.of(
                "s3Result", "",
                "dynamoResult", "",
                "snsResult", ""));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void passesWithDifferentValues() {
        Task task = taskWith(Map.of(
                "s3Result", "data/report.pdf",
                "dynamoResult", "doc-999",
                "snsResult", "sns-xyz"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
