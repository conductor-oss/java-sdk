package multimodalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessImageWorkerTest {

    private final ProcessImageWorker worker = new ProcessImageWorker();

    @Test
    void taskDefName() {
        assertEquals("mm_process_image", worker.getTaskDefName());
    }

    @Test
    void returnsTwoFeatureSets() {
        List<Map<String, String>> imageRefs = new ArrayList<>();
        imageRefs.add(new HashMap<>(Map.of("imageId", "img-001", "url", "https://example.com/img1.png")));
        imageRefs.add(new HashMap<>(Map.of("imageId", "img-002", "url", "https://example.com/img2.jpg")));

        Task task = taskWith(new HashMap<>(Map.of("imageRefs", imageRefs)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("imageFeatures");
        assertNotNull(features);
        assertEquals(2, features.size());
    }

    @Test
    void featureContainsImageIdObjectsLayoutCaption() {
        Task task = taskWith(new HashMap<>(Map.of(
                "imageRefs", List.of(Map.of("imageId", "img-001"))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("imageFeatures");
        Map<String, Object> first = features.get(0);

        assertEquals("img-001", first.get("imageId"));
        assertNotNull(first.get("objects"));
        assertNotNull(first.get("layout"));
        assertNotNull(first.get("caption"));
    }

    @Test
    void secondFeatureHasCorrectImageId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "imageRefs", List.of(Map.of("imageId", "img-002"))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("imageFeatures");
        assertEquals("img-002", features.get(1).get("imageId"));
    }

    @Test
    void handlesNullImageRefs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("imageFeatures"));
    }

    @Test
    void handlesEmptyImageRefs() {
        Task task = taskWith(new HashMap<>(Map.of(
                "imageRefs", new ArrayList<>()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("imageFeatures");
        assertNotNull(features);
        assertEquals(2, features.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
