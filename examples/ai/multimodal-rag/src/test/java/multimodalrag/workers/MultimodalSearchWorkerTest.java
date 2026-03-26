package multimodalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultimodalSearchWorkerTest {

    private final MultimodalSearchWorker worker = new MultimodalSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("mm_multimodal_search", worker.getTaskDefName());
    }

    @Test
    void returnsFourSearchResults() {
        List<Double> embedding = List.of(0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7);
        List<Map<String, Object>> imageFeatures = List.of(
                new HashMap<>(Map.of("imageId", "img-001", "caption", "test"))
        );
        List<Map<String, Object>> audioFeatures = List.of(
                new HashMap<>(Map.of("audioId", "aud-001", "transcript", "test"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "textEmbedding", embedding,
                "imageFeatures", imageFeatures,
                "audioFeatures", audioFeatures
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) result.getOutputData().get("searchResults");
        assertNotNull(searchResults);
        assertEquals(4, searchResults.size());
    }

    @Test
    void resultsContainDifferentModalities() {
        Task task = taskWith(new HashMap<>(Map.of(
                "textEmbedding", List.of(0.1, 0.2),
                "imageFeatures", List.of(),
                "audioFeatures", List.of()
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) result.getOutputData().get("searchResults");

        List<String> modalities = searchResults.stream()
                .map(r -> (String) r.get("modality"))
                .toList();
        assertTrue(modalities.contains("text"));
        assertTrue(modalities.contains("image"));
        assertTrue(modalities.contains("audio"));
    }

    @Test
    void resultsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "textEmbedding", List.of(0.1, 0.2)
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) result.getOutputData().get("searchResults");

        for (Map<String, Object> sr : searchResults) {
            assertNotNull(sr.get("resultId"));
            assertNotNull(sr.get("modality"));
            assertNotNull(sr.get("score"));
            assertNotNull(sr.get("content"));
        }
    }

    @Test
    void handlesNullTextEmbedding() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("searchResults"));
    }

    @Test
    void handlesNullImageAndAudioFeatures() {
        Task task = taskWith(new HashMap<>(Map.of(
                "textEmbedding", List.of(0.1, 0.2)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) result.getOutputData().get("searchResults");
        assertEquals(4, searchResults.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
