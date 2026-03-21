package endorsementprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edp_assess";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [assess] Change impact assessed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("impact", java.util.Map.of("riskChange", "slight-increase", "eligible", true));
        return result;
    }
}
