package schemaevolution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates transform operations from detected schema changes.
 * Input: changes, currentSchema
 * Output: transforms, transformCount
 */
public class GenerateTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_generate_transform";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> changes = (List<Map<String, Object>>) task.getInputData().get("changes");
        if (changes == null) {
            changes = List.of();
        }

        List<Map<String, Object>> transforms = new ArrayList<>();
        for (Map<String, Object> c : changes) {
            String type = (String) c.get("type");
            Map<String, Object> t = new LinkedHashMap<>();
            switch (type) {
                case "ADD_FIELD":
                    t.put("action", "addColumn"); t.put("field", c.get("field")); t.put("default", null);
                    break;
                case "RENAME_FIELD":
                    t.put("action", "renameColumn"); t.put("from", c.get("from")); t.put("to", c.get("to"));
                    break;
                case "CHANGE_TYPE":
                    t.put("action", "castType"); t.put("field", c.get("field")); t.put("targetType", c.get("to"));
                    break;
                case "DROP_FIELD":
                    t.put("action", "dropColumn"); t.put("field", c.get("field"));
                    break;
                case "ADD_DEFAULT":
                    t.put("action", "setDefault"); t.put("field", c.get("field")); t.put("value", c.get("defaultValue"));
                    break;
                default:
                    t.put("action", "unknown");
                    break;
            }
            transforms.add(t);
        }

        System.out.println("  [generate] Generated " + transforms.size() + " transform operations");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transforms", transforms);
        result.getOutputData().put("transformCount", transforms.size());
        return result;
    }
}
