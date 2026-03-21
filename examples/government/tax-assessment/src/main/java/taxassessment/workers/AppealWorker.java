package taxassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AppealWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "txa_appeal";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [appeal] Appeal window opened — deadline: 2024-06-30");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("appealDeadline", "2024-06-30");
        result.getOutputData().put("appealOpen", true);
        return result;
    }
}
