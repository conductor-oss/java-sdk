package druginteraction.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.stream.Collectors;

public class FlagConflictsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "drg_flag_conflicts"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> interactions =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("interactions", List.of());
        List<Map<String, Object>> conflicts = interactions.stream()
                .filter(i -> "major".equals(i.get("severity")) || "moderate".equals(i.get("severity")))
                .collect(Collectors.toList());
        System.out.println("  [flag] " + conflicts.size() + " conflict(s) found out of " + interactions.size() + " pairs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("conflicts", conflicts);
        output.put("conflictCount", conflicts.size());
        result.setOutputData(output);
        return result;
    }
}
