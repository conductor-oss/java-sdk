package aiimagegeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnhanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aig_enhance";
    }

    @Override
    public TaskResult execute(Task task) {

        String imageId = (String) task.getInputData().get("imageId");
        System.out.printf("  [enhance] Image %s upscaled and color-corrected%n", imageId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enhancedImageId", "IMG-ai-image-generation-001-E");
        result.getOutputData().put("upscaleFactor", 2);
        return result;
    }
}
