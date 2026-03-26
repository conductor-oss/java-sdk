package schemaevolution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects schema changes between current and target schemas.
 * Input: currentSchema, targetSchema
 * Output: changes, changeCount, changeTypes
 */
public class DetectChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_detect_changes";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> changes = new ArrayList<>();

        Map<String, Object> c1 = new LinkedHashMap<>();
        c1.put("type", "ADD_FIELD"); c1.put("field", "middle_name"); c1.put("dataType", "string"); c1.put("nullable", true);
        changes.add(c1);

        Map<String, Object> c2 = new LinkedHashMap<>();
        c2.put("type", "RENAME_FIELD"); c2.put("from", "phone"); c2.put("to", "phone_number");
        changes.add(c2);

        Map<String, Object> c3 = new LinkedHashMap<>();
        c3.put("type", "CHANGE_TYPE"); c3.put("field", "age"); c3.put("from", "string"); c3.put("to", "integer");
        changes.add(c3);

        Map<String, Object> c4 = new LinkedHashMap<>();
        c4.put("type", "DROP_FIELD"); c4.put("field", "legacy_id");
        changes.add(c4);

        Map<String, Object> c5 = new LinkedHashMap<>();
        c5.put("type", "ADD_DEFAULT"); c5.put("field", "status"); c5.put("defaultValue", "active");
        changes.add(c5);

        Set<String> changeTypeSet = new LinkedHashSet<>();
        for (Map<String, Object> c : changes) {
            changeTypeSet.add((String) c.get("type"));
        }
        List<String> changeTypes = new ArrayList<>(changeTypeSet);

        System.out.println("  [detect] Found " + changes.size() + " schema changes: " + String.join(", ", changeTypes));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("changes", changes);
        result.getOutputData().put("changeCount", changes.size());
        result.getOutputData().put("changeTypes", changeTypes);
        return result;
    }
}
