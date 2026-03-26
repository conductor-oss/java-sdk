package ocrpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PreprocessImageWorkerTest {

    private final PreprocessImageWorker worker = new PreprocessImageWorker();

    @Test
    void taskDefName() {
        assertEquals("oc_preprocess_image", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedImageData() {
        Task task = taskWith(Map.of("imageUrl", "s3://docs/scan.tiff", "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("preprocessed_data", result.getOutputData().get("processedImage"));
        assertEquals(2.3, result.getOutputData().get("deskewAngle"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsEnhancementsList() {
        Task task = taskWith(Map.of("imageUrl", "test.jpg", "documentType", "receipt"));
        TaskResult result = worker.execute(task);

        List<String> enhancements = (List<String>) result.getOutputData().get("enhancementsApplied");
        assertNotNull(enhancements);
        assertEquals(3, enhancements.size());
        assertTrue(enhancements.contains("binarize"));
        assertTrue(enhancements.contains("contrast"));
        assertTrue(enhancements.contains("deskew"));
    }

    @Test
    void handlesDefaultValues() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("processedImage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
