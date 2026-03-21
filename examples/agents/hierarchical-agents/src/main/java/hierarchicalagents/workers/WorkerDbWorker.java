package hierarchicalagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database developer worker that builds the schema and migrations based on the
 * DB task from the lead and the API output.
 */
public class WorkerDbWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hier_worker_db";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> dbTask = (Map<String, Object>) task.getInputData().get("task");
        // apiOutput is available but used for context only
        task.getInputData().get("apiOutput");

        List<String> tableNames = dbTask != null ? (List<String>) dbTask.get("tables") : List.of();

        System.out.println("  [hier_worker_db] Building " + tableNames.size() + " tables");

        List<Map<String, Object>> tables = tableNames.stream().map(name -> {
            Map<String, Object> table = new LinkedHashMap<>();
            table.put("name", name);
            table.put("status", "created");
            return table;
        }).toList();

        Map<String, Object> dbResult = new LinkedHashMap<>();
        dbResult.put("tables", tables);
        dbResult.put("migrations", 3);
        dbResult.put("seedData", true);
        dbResult.put("linesOfCode", 180);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", dbResult);
        return result;
    }
}
