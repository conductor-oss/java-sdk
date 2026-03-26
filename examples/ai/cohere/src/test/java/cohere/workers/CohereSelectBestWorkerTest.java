package cohere.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CohereSelectBestWorkerTest {

    private final CohereSelectBestWorker worker = new CohereSelectBestWorker();

    @Test
    void taskDefName() {
        assertEquals("cohere_select_best", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectsGenerationWithHighestLikelihood() {
        List<Map<String, Object>> generations = List.of(
                Map.of("text", "Gen zero text", "likelihood", -1.82),
                Map.of("text", "Gen one text", "likelihood", -1.65),
                Map.of("text", "Gen two text", "likelihood", -1.91));

        Task task = taskWith(new HashMap<>(Map.of("generations", generations)));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Gen one text", result.getOutputData().get("bestGeneration"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsAllTexts() {
        List<Map<String, Object>> generations = List.of(
                Map.of("text", "Alpha", "likelihood", -2.0),
                Map.of("text", "Beta", "likelihood", -1.5),
                Map.of("text", "Gamma", "likelihood", -1.8));

        Task task = taskWith(new HashMap<>(Map.of("generations", generations)));

        TaskResult result = worker.execute(task);

        List<String> allTexts = (List<String>) result.getOutputData().get("allTexts");
        assertEquals(3, allTexts.size());
        assertEquals("Alpha", allTexts.get(0));
        assertEquals("Beta", allTexts.get(1));
        assertEquals("Gamma", allTexts.get(2));
    }

    @Test
    void selectsBestFromSingleGeneration() {
        List<Map<String, Object>> generations = List.of(
                Map.of("text", "Only option", "likelihood", -0.5));

        Task task = taskWith(new HashMap<>(Map.of("generations", generations)));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Only option", result.getOutputData().get("bestGeneration"));
    }

    @Test
    void selectsFirstWhenTied() {
        List<Map<String, Object>> generations = List.of(
                Map.of("text", "First", "likelihood", -1.0),
                Map.of("text", "Second", "likelihood", -1.0));

        Task task = taskWith(new HashMap<>(Map.of("generations", generations)));

        TaskResult result = worker.execute(task);

        assertEquals("First", result.getOutputData().get("bestGeneration"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void worksWithCohereGenerateWorkerOutput() {
        // Simulate the full pipeline: generate worker output fed into select_best
        CohereGenerateWorker generateWorker = new CohereGenerateWorker();
        Task genTask = new Task();
        genTask.setStatus(Task.Status.IN_PROGRESS);
        genTask.setInputData(new HashMap<>(Map.of("requestBody", Map.of())));
        TaskResult genResult = generateWorker.execute(genTask);

        Map<String, Object> apiResponse = (Map<String, Object>) genResult.getOutputData().get("apiResponse");
        List<Map<String, Object>> generations = (List<Map<String, Object>>) apiResponse.get("generations");

        Task selectTask = taskWith(new HashMap<>(Map.of("generations", generations)));
        TaskResult selectResult = worker.execute(selectTask);

        assertEquals(TaskResult.Status.COMPLETED, selectResult.getStatus());
        // Generation index 1 has likelihood -1.65 (highest/least negative)
        String bestText = (String) selectResult.getOutputData().get("bestGeneration");
        assertTrue(bestText.contains("SmartBoard Pro brings AI-driven insights"));

        List<String> allTexts = (List<String>) selectResult.getOutputData().get("allTexts");
        assertEquals(3, allTexts.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
