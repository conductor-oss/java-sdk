package aiimagegeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aig_validate";
    }

    @Override
    public TaskResult execute(Task task) {

        String imageId = (String) task.getInputData().get("imageId");
        System.out.printf("  [validate] Image %s quality: 0.94, safe content%n", imageId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("qualityScore", 0.94);
        result.getOutputData().put("safeContent", true);
        result.getOutputData().put("artifacts", 0);
        return result;
    }
}
