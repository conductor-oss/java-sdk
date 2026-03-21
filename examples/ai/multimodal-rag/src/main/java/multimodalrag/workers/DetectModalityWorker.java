package multimodalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that detects modalities from a question and its attachments.
 * Returns detected modalities (text, image, audio), text content,
 * image references, and audio references.
 */
public class DetectModalityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mm_detect_modality";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, String>> attachments =
                (List<Map<String, String>>) task.getInputData().get("attachments");
        if (attachments == null) {
            attachments = List.of();
        }

        List<String> detectedModalities = List.of("text", "image", "audio");
        String textContent = "Extracted text content from question: " + question;

        List<Map<String, String>> imageRefs = List.of(
                Map.of("imageId", "img-001", "url", "https://storage.example.com/img-001.png",
                        "format", "png"),
                Map.of("imageId", "img-002", "url", "https://storage.example.com/img-002.jpg",
                        "format", "jpeg")
        );

        List<Map<String, String>> audioRefs = List.of(
                Map.of("audioId", "aud-001", "url", "https://storage.example.com/aud-001.wav",
                        "format", "wav", "durationSec", "45")
        );

        System.out.println("  [detect_modality] Detected modalities: " + detectedModalities
                + " from " + attachments.size() + " attachments");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detectedModalities", detectedModalities);
        result.getOutputData().put("textContent", textContent);
        result.getOutputData().put("imageRefs", imageRefs);
        result.getOutputData().put("audioRefs", audioRefs);
        return result;
    }
}
