package dynamicfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepares the dynamic task list and input map for FORK_JOIN_DYNAMIC.
 *
 * Takes a list of URLs and generates:
 * - dynamicTasks: array of task definitions (name, taskReferenceName, type)
 * - dynamicTasksInput: map of taskReferenceName -> input for each task
 *
 * Each dynamic task is a "df_fetch_url" SIMPLE task with a unique reference name.
 */
public class PrepareTasksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "df_prepare_tasks";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) task.getInputData().get("urls");

        if (urls == null) {
            urls = List.of();
        }

        System.out.println("  [df_prepare_tasks] Preparing " + urls.size() + " dynamic tasks");

        List<Map<String, Object>> dynamicTasks = new ArrayList<>();
        Map<String, Map<String, Object>> dynamicTasksInput = new LinkedHashMap<>();

        for (int i = 0; i < urls.size(); i++) {
            String refName = "fetch_" + i + "_ref";

            // Task definition for the dynamic fork
            Map<String, Object> taskDef = new LinkedHashMap<>();
            taskDef.put("name", "df_fetch_url");
            taskDef.put("taskReferenceName", refName);
            taskDef.put("type", "SIMPLE");
            dynamicTasks.add(taskDef);

            // Input for this task
            Map<String, Object> taskInput = new HashMap<>();
            taskInput.put("url", urls.get(i));
            taskInput.put("index", i);
            dynamicTasksInput.put(refName, taskInput);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dynamicTasks", dynamicTasks);
        result.getOutputData().put("dynamicTasksInput", dynamicTasksInput);
        return result;
    }
}
