package remotemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class NormalActionWorker implements Worker {

    @Override
    public String getTaskDefName() { return "rpm_normal_action"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [normal] All vitals within range for " + task.getInputData().get("patientId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("action", "continue_monitoring");
        output.put("nextReading", "4 hours");
        result.setOutputData(output);
        return result;
    }
}
