package remotemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class CollectVitalsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "rpm_collect_vitals"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [collect] Reading vitals from device " + task.getInputData().get("deviceId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> vitals = new LinkedHashMap<>();
        vitals.put("heartRate", 92);
        vitals.put("bloodPressure", Map.of("systolic", 155, "diastolic", 95));
        vitals.put("oxygenSaturation", 94);
        vitals.put("glucose", 185);
        vitals.put("temperature", 98.8);
        output.put("vitals", vitals);
        output.put("timestamp", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
