package dataanonymization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generalizes quasi-identifier and selected direct-identifier fields.
 * Input: dataset, piiFields
 * Output: generalized, generalizedCount
 */
public class GeneralizeDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "an_generalize_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("dataset");
        List<Map<String, Object>> pii = (List<Map<String, Object>>) task.getInputData().get("piiFields");
        if (data == null) data = List.of();
        if (pii == null) pii = List.of();

        Set<String> generalizeFields = pii.stream()
                .filter(f -> "generalize".equals(f.get("action")))
                .map(f -> (String) f.get("field"))
                .collect(Collectors.toSet());

        List<Map<String, Object>> generalized = new ArrayList<>();
        for (Map<String, Object> record : data) {
            Map<String, Object> r = new LinkedHashMap<>(record);
            if (generalizeFields.contains("name") && r.get("name") != null) {
                String name = (String) r.get("name");
                r.put("name", name.substring(0, 1) + "***");
            }
            if (generalizeFields.contains("age") && r.get("age") != null) {
                int age = ((Number) r.get("age")).intValue();
                int bucket = (age / 10) * 10;
                r.put("age", bucket + "-" + (bucket + 9));
            }
            if (generalizeFields.contains("zipCode") && r.get("zipCode") != null) {
                String zip = (String) r.get("zipCode");
                r.put("zipCode", zip.substring(0, 3) + "**");
            }
            generalized.add(r);
        }

        System.out.println("  [generalize] Generalized " + generalizeFields.size() + " fields: " + String.join(", ", generalizeFields));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("generalized", generalized);
        result.getOutputData().put("generalizedCount", generalizeFields.size());
        return result;
    }
}
