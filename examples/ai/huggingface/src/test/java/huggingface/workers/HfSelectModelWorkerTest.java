package huggingface.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HfSelectModelWorkerTest {

    private final HfSelectModelWorker worker = new HfSelectModelWorker();

    @Test
    void taskDefName() {
        assertEquals("hf_select_model", worker.getTaskDefName());
    }

    @Test
    void selectsSummarizationModel() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Some article text",
                "task", "summarization"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("facebook/bart-large-cnn", result.getOutputData().get("modelId"));
        assertEquals("Some article text", result.getOutputData().get("formattedInput"));
    }

    @Test
    void selectsTextGenerationModel() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Generate something",
                "task", "text-generation"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("mistralai/Mixtral-8x7B-Instruct-v0.1", result.getOutputData().get("modelId"));
    }

    @Test
    void selectsSentimentAnalysisModel() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "I love this product",
                "task", "sentiment-analysis"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("distilbert-base-uncased-finetuned-sst-2-english",
                result.getOutputData().get("modelId"));
    }

    @Test
    void selectsTranslationModel() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Hello world",
                "task", "translation"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("Helsinki-NLP/opus-mt-en-fr", result.getOutputData().get("modelId"));
    }

    @Test
    void defaultsToTextGenerationForUnknownTask() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Some text",
                "task", "unknown-task"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("mistralai/Mixtral-8x7B-Instruct-v0.1", result.getOutputData().get("modelId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsParameters() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Test",
                "task", "summarization"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> parameters = (Map<String, Object>) result.getOutputData().get("parameters");
        assertNotNull(parameters);
        assertEquals(250, parameters.get("max_new_tokens"));
        assertEquals(0.6, parameters.get("temperature"));
        assertEquals(0.9, parameters.get("top_p"));
        assertEquals(1.1, parameters.get("repetition_penalty"));
        assertEquals(true, parameters.get("do_sample"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputContainsOptions() {
        Task task = taskWith(new HashMap<>(Map.of(
                "text", "Test",
                "task", "summarization"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> options = (Map<String, Object>) result.getOutputData().get("options");
        assertNotNull(options);
        assertEquals(true, options.get("wait_for_model"));
        assertEquals(false, options.get("use_cache"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
