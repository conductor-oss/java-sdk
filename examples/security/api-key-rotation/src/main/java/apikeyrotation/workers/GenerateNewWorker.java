package apikeyrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateNewWorker implements Worker {

    @Override public String getTaskDefName() { return "akr_generate_new"; }

    @Override
    public TaskResult execute(Task task) {
        String service = (String) task.getInputData().get("service");
        if (service == null) service = "unknown";

        System.out.println("  [generate] New API key generated for " + service);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("generate_newId", "GENERATE_NEW-1374");
        result.getOutputData().put("success", true);
        return result;
    }
}
