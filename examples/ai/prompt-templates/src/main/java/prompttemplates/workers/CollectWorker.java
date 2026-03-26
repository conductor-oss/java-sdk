package prompttemplates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Collects and logs the results of the prompt template pipeline.
 */
public class CollectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pt_collect";
    }

    @Override
    public TaskResult execute(Task task) {
        String templateId = (String) task.getInputData().get("templateId");
        Object versionObj = task.getInputData().get("version");
        String response = (String) task.getInputData().get("response");

        System.out.println("  [pt_collect] Logging result for template " + templateId
                + " v" + versionObj + ": " + response.substring(0, Math.min(50, response.length())) + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
