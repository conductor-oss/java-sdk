package fanoutfanin.workers;

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
 * Takes an images list and generates:
 * - dynamicTasks: array of task definitions (name, taskReferenceName, type)
 * - dynamicTasksInput: map of taskReferenceName -> input for each task
 *
 * Each dynamic task is an "fo_process_image" SIMPLE task with a unique reference name
 * like "img_0_ref", "img_1_ref", etc.
 */
public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_prepare";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> images = (List<Map<String, Object>>) task.getInputData().get("images");

        if (images == null) {
            images = List.of();
        }

        System.out.println("  [fo_prepare] Preparing " + images.size() + " dynamic tasks");

        List<Map<String, Object>> dynamicTasks = new ArrayList<>();
        Map<String, Map<String, Object>> dynamicTasksInput = new LinkedHashMap<>();

        for (int i = 0; i < images.size(); i++) {
            String refName = "img_" + i + "_ref";

            // Task definition for the dynamic fork
            Map<String, Object> taskDef = new LinkedHashMap<>();
            taskDef.put("name", "fo_process_image");
            taskDef.put("taskReferenceName", refName);
            taskDef.put("type", "SIMPLE");
            dynamicTasks.add(taskDef);

            // Input for this task
            Map<String, Object> taskInput = new HashMap<>();
            taskInput.put("image", images.get(i));
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
