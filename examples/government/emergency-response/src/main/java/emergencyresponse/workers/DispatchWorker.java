package emergencyresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class DispatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_dispatch";
    }

    @Override
    public TaskResult execute(Task task) {
        String location = (String) task.getInputData().get("location");
        String severity = (String) task.getInputData().get("severity");
        System.out.printf("  [dispatch] 3 units dispatched to %s (severity: %s)%n", location, severity);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("units", List.of("Engine-7", "Rescue-3", "Medic-1"));
        result.getOutputData().put("dispatchTime", "14:32:00");
        return result;
    }
}
