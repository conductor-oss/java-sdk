package mentalhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class TrackProgressWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mh_track_progress"; }

    @Override
    public TaskResult execute(Task task) {
        int baseline = 0;
        try { baseline = Integer.parseInt(String.valueOf(task.getInputData().getOrDefault("baselineScore", "0"))); }
        catch (NumberFormatException ignored) {}
        System.out.println("  [track] Progress tracking activated — baseline PHQ-9: " + baseline);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("trackingActive", true);
        output.put("measures", List.of("PHQ-9", "GAD-7"));
        output.put("checkInSchedule", "biweekly");
        output.put("baselinePhq9", baseline);
        result.setOutputData(output);
        return result;
    }
}
