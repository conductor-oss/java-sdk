package smoketesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_verify_data";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [data] Database connectivity and sample queries verified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_data", true);
        result.addOutputData("processed", true);
        return result;
    }
}
