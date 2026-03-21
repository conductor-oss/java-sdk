package capacitymgmttelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmt_verify";
    }

    @Override
    public TaskResult execute(Task task) {

        String region = (String) task.getInputData().get("region");
        System.out.printf("  [verify] Region %s capacity verified%n", region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("newUtilization", 52);
        return result;
    }
}
