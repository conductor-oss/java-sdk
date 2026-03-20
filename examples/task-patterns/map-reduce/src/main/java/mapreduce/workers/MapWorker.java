package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MAP phase: splits log files into parallel analysis tasks.
 *
 * Takes a list of log files and generates:
 * - dynamicTasks: array of task definitions (name, taskReferenceName, type)
 * - dynamicTasksInput: map of taskReferenceName -> input for each task
 *
 * Each dynamic task is an "mr_analyze_log" SIMPLE task with reference name "log_N_ref".
 */
public class MapWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mr_map";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> logFiles =
                (List<Map<String, Object>>) task.getInputData().get("logFiles");

        if (logFiles == null) {
            logFiles = List.of();
        }

        System.out.println("  [mr_map] Splitting " + logFiles.size() + " log files into parallel tasks");

        List<Map<String, Object>> dynamicTasks = new ArrayList<>();
        Map<String, Map<String, Object>> dynamicTasksInput = new LinkedHashMap<>();

        for (int i = 0; i < logFiles.size(); i++) {
            String refName = "log_" + i + "_ref";

            // Task definition for the dynamic fork
            Map<String, Object> taskDef = new LinkedHashMap<>();
            taskDef.put("name", "mr_analyze_log");
            taskDef.put("taskReferenceName", refName);
            taskDef.put("type", "SIMPLE");
            dynamicTasks.add(taskDef);

            // Input for this task
            Map<String, Object> taskInput = new HashMap<>();
            taskInput.put("logFile", logFiles.get(i));
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
