package xmlparsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts id, name, price (as double), and category from each parsed element.
 * Input: elements (list of parsed element maps)
 * Output: records (list of extracted record maps), fieldCount
 */
public class ExtractFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xp_extract_fields";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> elements = (List<Map<String, Object>>) task.getInputData().get("elements");
        if (elements == null) {
            elements = List.of();
        }

        System.out.println("  [xp_extract_fields] Extracting fields from " + elements.size() + " elements");

        List<Map<String, Object>> records = new ArrayList<>();
        int fieldCount = 0;

        for (Map<String, Object> element : elements) {
            Map<String, Object> attributes = (Map<String, Object>) element.get("attributes");
            Map<String, Object> children = (Map<String, Object>) element.get("children");

            Map<String, Object> record = new LinkedHashMap<>();

            String id = attributes != null ? (String) attributes.get("id") : null;
            record.put("id", id);
            fieldCount++;

            String name = children != null ? (String) children.get("name") : null;
            record.put("name", name);
            fieldCount++;

            String priceStr = children != null ? (String) children.get("price") : null;
            double price = priceStr != null ? Double.parseDouble(priceStr) : 0.0;
            record.put("price", price);
            fieldCount++;

            String category = children != null ? (String) children.get("category") : null;
            record.put("category", category);
            fieldCount++;

            records.add(record);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("fieldCount", fieldCount);
        return result;
    }
}
