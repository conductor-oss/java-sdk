package datacatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Classifies data assets by PII and category.
 * Input: assets, rules
 * Output: classified, classificationSummary
 */
public class ClassifyDataWorker implements Worker {

    private static final List<String> PII_COLUMNS = List.of("email", "phone", "address", "ip_address");

    @Override
    public String getTaskDefName() {
        return "cg_classify_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) task.getInputData().get("assets");
        if (assets == null) {
            assets = List.of();
        }

        List<Map<String, Object>> classified = new ArrayList<>();
        int piiCount = 0;
        int operational = 0;
        int analytics = 0;
        int reporting = 0;

        for (Map<String, Object> asset : assets) {
            Map<String, Object> c = new LinkedHashMap<>(asset);
            List<String> columns = (List<String>) asset.getOrDefault("columns", List.of());
            boolean hasPII = columns.stream().anyMatch(PII_COLUMNS::contains);
            String schema = (String) asset.getOrDefault("schema", "");
            String category;
            if ("analytics".equals(schema)) {
                category = "analytics";
                analytics++;
            } else if ("reporting".equals(schema)) {
                category = "reporting";
                reporting++;
            } else {
                category = "operational";
                operational++;
            }
            c.put("hasPII", hasPII);
            c.put("category", category);
            c.put("sensitivity", hasPII ? "high" : "low");
            if (hasPII) piiCount++;
            classified.add(c);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("operational", operational);
        summary.put("analytics", analytics);
        summary.put("reporting", reporting);
        summary.put("piiAssets", piiCount);

        System.out.println("  [classify] Classified " + classified.size() + " assets — PII detected in " + piiCount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classified", classified);
        result.getOutputData().put("classificationSummary", summary);
        return result;
    }
}
