package aiimagegeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aig_deliver";
    }

    @Override
    public TaskResult execute(Task task) {

        String imageId = (String) task.getInputData().get("imageId");
        System.out.printf("  [deliver] Image %s delivered as png%n", imageId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("delivered", true);
        result.getOutputData().put("url", "/images/IMG-ai-image-generation-001-E.png");
        result.getOutputData().put("sizeKB", 2048);
        return result;
    }
}
