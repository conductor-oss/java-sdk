package huggingface.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HfFormatResultWorkerTest {

    private final HfFormatResultWorker worker = new HfFormatResultWorker();

    @Test
    void taskDefName() {
        assertEquals("hf_format_result", worker.getTaskDefName());
    }

    @Test
    void formatsSummarizationResult() {
        List<Map<String, Object>> rawOutput = List.of(
                new HashMap<>(Map.of("summary_text", "This is a summary of the article."))
        );
        Task task = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "summarization"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("This is a summary of the article.", result.getOutputData().get("result"));
    }

    @Test
    void formatsSentimentAnalysisResult() {
        List<Map<String, Object>> rawOutput = List.of(
                new HashMap<>(Map.of("label", "POSITIVE"))
        );
        Task task = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "sentiment-analysis"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("POSITIVE", result.getOutputData().get("result"));
    }

    @Test
    void formatsTextGenerationResult() {
        List<Map<String, Object>> rawOutput = List.of(
                new HashMap<>(Map.of("generated_text", "Once upon a time in a land far away..."))
        );
        Task task = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "text-generation"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("Once upon a time in a land far away...", result.getOutputData().get("result"));
    }

    @Test
    void fallsBackToStringRepresentationWhenNoGeneratedText() {
        List<Map<String, Object>> rawOutput = List.of(
                new HashMap<>(Map.of("other_field", "some value"))
        );
        Task task = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "text-generation"
        )));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        String resultStr = (String) result.getOutputData().get("result");
        assertTrue(resultStr.contains("some value"));
    }

    @Test
    void outputIsDeterministic() {
        List<Map<String, Object>> rawOutput = List.of(
                new HashMap<>(Map.of("summary_text", "Deterministic summary"))
        );
        Task task1 = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "summarization"
        )));
        Task task2 = taskWith(new HashMap<>(Map.of(
                "rawOutput", rawOutput,
                "task", "summarization"
        )));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
