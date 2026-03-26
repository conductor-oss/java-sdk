package amazonbedrock.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BedrockParseOutputWorkerTest {

    private final BedrockParseOutputWorker worker = new BedrockParseOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("bedrock_parse_output", worker.getTaskDefName());
    }

    @Test
    void extractsClassificationText() {
        String expectedText = "Classification: URGENT — Compliance Risk\n\nDetails here.";
        Task task = taskWith(Map.of(
                "responseBody", Map.of(
                        "content", List.of(
                                Map.of("type", "text", "text", expectedText)
                        )
                ),
                "metrics", Map.of("latencyMs", 1850)
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(expectedText, result.getOutputData().get("classification"));
    }

    @Test
    void handlesLongClassificationText() {
        String longText = "Classification: LOW PRIORITY\n\n" + "A".repeat(200);
        Task task = taskWith(Map.of(
                "responseBody", Map.of(
                        "content", List.of(
                                Map.of("type", "text", "text", longText)
                        )
                ),
                "metrics", Map.of("latencyMs", 500)
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(longText, result.getOutputData().get("classification"));
    }

    @Test
    void outputContainsOnlyClassificationField() {
        Task task = taskWith(Map.of(
                "responseBody", Map.of(
                        "content", List.of(
                                Map.of("type", "text", "text", "Some text")
                        )
                ),
                "metrics", Map.of("latencyMs", 100)
        ));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("classification"));
        assertEquals("Some text", result.getOutputData().get("classification"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
