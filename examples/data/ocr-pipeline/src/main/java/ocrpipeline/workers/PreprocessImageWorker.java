package ocrpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Preprocesses an image for OCR: deskew, binarize, contrast-enhance.
 * Input: imageUrl, documentType
 * Output: processedImage, deskewAngle, enhancementsApplied
 */
public class PreprocessImageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oc_preprocess_image";
    }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().getOrDefault("imageUrl", "unknown.jpg");
        String docType = (String) task.getInputData().getOrDefault("documentType", "generic");

        System.out.println("  [preprocess] Image " + url + ": deskewed, binarized, contrast-enhanced for " + docType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedImage", "preprocessed_data");
        result.getOutputData().put("deskewAngle", 2.3);
        result.getOutputData().put("enhancementsApplied", List.of("binarize", "contrast", "deskew"));
        return result;
    }
}
