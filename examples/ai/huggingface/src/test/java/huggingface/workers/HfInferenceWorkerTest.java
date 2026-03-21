package huggingface.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HfInferenceWorkerTest {

    private final HfInferenceWorker worker = new HfInferenceWorker();

    @Test
    void taskDefName() {
        assertEquals("hf_inference", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDemoSummarizationOutput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "facebook/bart-large-cnn",
                "inputs", "Some article text"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, String>> rawOutput =
                (List<Map<String, String>>) result.getOutputData().get("rawOutput");
        assertNotNull(rawOutput);
        assertEquals(1, rawOutput.size());
        assertTrue(rawOutput.get(0).containsKey("summary_text"));

        // In demo mode, text starts with [DEMO] and contains the key phrase
        if (System.getenv("HUGGINGFACE_TOKEN") == null || System.getenv("HUGGINGFACE_TOKEN").isBlank()) {
            assertTrue(rawOutput.get(0).get("summary_text").contains("hybrid work models"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("modelId", "facebook/bart-large-cnn")));
        Task task2 = taskWith(new HashMap<>(Map.of("modelId", "facebook/bart-large-cnn")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        // In demo mode, output should be deterministic
        if (System.getenv("HUGGINGFACE_TOKEN") == null || System.getenv("HUGGINGFACE_TOKEN").isBlank()) {
            List<Map<String, String>> out1 =
                    (List<Map<String, String>>) result1.getOutputData().get("rawOutput");
            List<Map<String, String>> out2 =
                    (List<Map<String, String>>) result2.getOutputData().get("rawOutput");

            assertEquals(out1.get(0).get("summary_text"), out2.get(0).get("summary_text"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
