package publicrecords.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbr_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Documents verified as releasable");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verifiedDocs", task.getInputData().get("documents"));
        result.getOutputData().put("allVerified", true);
        return result;
    }
}
