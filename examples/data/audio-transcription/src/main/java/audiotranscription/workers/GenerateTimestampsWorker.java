package audiotranscription.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates timestamps for transcript segments.
 * Input: transcript (string), segments (list)
 * Output: timestamped (list), segmentCount (int)
 */
public class GenerateTimestampsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "au_generate_timestamps";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> segments = (List<Map<String, Object>>) task.getInputData().get("segments");
        if (segments == null) {
            segments = List.of();
        }

        List<Map<String, Object>> timestamped = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Map<String, Object> copy = new HashMap<>(segments.get(i));
            copy.put("startTime", (i * 2) + ":" + String.format("%02d", i * 15));
            copy.put("endTime", (i * 2 + 1) + ":" + String.format("%02d", (i + 1) * 12));
            timestamped.add(copy);
        }

        System.out.println("  [timestamps] Generated timestamps for " + timestamped.size() + " segments");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timestamped", timestamped);
        result.getOutputData().put("segmentCount", timestamped.size());
        return result;
    }
}
