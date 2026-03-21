package supplychainmgmt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Sources materials from suppliers.
 * Input: materials
 * Output: sourcedMaterials, suppliersUsed
 */
public class SourceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "scm_source";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> materials =
                (List<Map<String, Object>>) task.getInputData().get("materials");
        if (materials == null) {
            materials = List.of();
        }

        System.out.println("  [source] Sourced " + materials.size() + " materials from suppliers");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sourcedMaterials", materials);
        result.getOutputData().put("suppliersUsed", 3);
        return result;
    }
}
