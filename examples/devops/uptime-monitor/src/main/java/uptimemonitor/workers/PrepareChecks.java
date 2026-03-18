package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Generates dynamic fork tasks for parallel endpoint checks.
 */
public class PrepareChecks implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_prepare_checks";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[uptime_prepare_checks] Preparing endpoint checks...");

        TaskResult result = new TaskResult(task);
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) task.getInputData().get("endpoints");

        if (endpoints == null || endpoints.isEmpty()) {
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("dynamicTasks", Collections.emptyList());
            result.getOutputData().put("dynamicTasksInput", Collections.emptyMap());
            return result;
        }

        List<Map<String, Object>> dynamicTasks = new ArrayList<>();
        Map<String, Object> dynamicTasksInput = new LinkedHashMap<>();

        for (int i = 0; i < endpoints.size(); i++) {
            Map<String, Object> ep = endpoints.get(i);
            String refName = "check_ep_" + i + "_ref";

            Map<String, Object> taskDef = new LinkedHashMap<>();
            taskDef.put("name", "uptime_check_endpoint");
            taskDef.put("taskReferenceName", refName);
            taskDef.put("type", "SIMPLE");
            dynamicTasks.add(taskDef);

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("url", ep.get("url"));
            input.put("name", ep.get("name"));
            input.put("expectedStatus", ep.get("expectedStatus"));
            input.put("timeout", ep.getOrDefault("timeout", 5000));
            dynamicTasksInput.put(refName, input);
        }

        System.out.println("  Prepared " + endpoints.size() + " endpoint checks");
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dynamicTasks", dynamicTasks);
        result.getOutputData().put("dynamicTasksInput", dynamicTasksInput);
        return result;
    }
}
