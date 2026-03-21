package gdprconsent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class UpdateSystemsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gdc_update_systems";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [update] 4 downstream systems updated with consent preferences");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("systemsUpdated", 4);
        result.getOutputData().put("systems", List.of("analytics", "email", "ads", "dmp"));
        return result;
    }
}
