package multimodalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectModalityWorkerTest {

    private final DetectModalityWorker worker = new DetectModalityWorker();

    @Test
    void taskDefName() {
        assertEquals("mm_detect_modality", worker.getTaskDefName());
    }

    @Test
    void detectsAllModalities() {
        List<Map<String, String>> attachments = new ArrayList<>();
        attachments.add(new HashMap<>(Map.of("type", "image", "url", "https://example.com/img.png")));
        attachments.add(new HashMap<>(Map.of("type", "audio", "url", "https://example.com/audio.wav")));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Describe the diagram and audio",
                "attachments", attachments
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> modalities = (List<String>) result.getOutputData().get("detectedModalities");
        assertNotNull(modalities);
        assertEquals(3, modalities.size());
        assertTrue(modalities.contains("text"));
        assertTrue(modalities.contains("image"));
        assertTrue(modalities.contains("audio"));
    }

    @Test
    void returnsTextContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is multimodal RAG?"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String textContent = (String) result.getOutputData().get("textContent");
        assertNotNull(textContent);
        assertTrue(textContent.contains("What is multimodal RAG?"));
    }

    @Test
    void returnsTwoImageRefs() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> imageRefs =
                (List<Map<String, String>>) result.getOutputData().get("imageRefs");
        assertNotNull(imageRefs);
        assertEquals(2, imageRefs.size());
        assertEquals("img-001", imageRefs.get(0).get("imageId"));
        assertEquals("img-002", imageRefs.get(1).get("imageId"));
    }

    @Test
    void returnsOneAudioRef() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> audioRefs =
                (List<Map<String, String>>) result.getOutputData().get("audioRefs");
        assertNotNull(audioRefs);
        assertEquals(1, audioRefs.size());
        assertEquals("aud-001", audioRefs.get(0).get("audioId"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("textContent"));
    }

    @Test
    void handlesNullAttachments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("detectedModalities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
