package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * Locates all data associated with a subject by scanning real data structures.
 *
 * Scans the provided data sources map for entries matching the subject ID.
 * If no data sources are provided, throws an error instead of returning
 * synthetic results.
 *
 * Input:
 *   - subjectId (String, required): the data subject identifier
 *   - dataSources (List of Maps, required): data structures to scan,
 *     each with keys: system, table, fields (List), records (List of Maps)
 *
 * Output:
 *   - dataLocations (List): locations where subject data was found
 *   - systemsScanned (int): number of systems scanned
 *   - fieldsMatched (int): total number of fields with subject data
 *   - auditLog (Map): timestamp, action, actor, result
 */
public class LocateDataWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_locate_data"; }

    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate required inputs
        String subjectId = getRequiredString(task, "subjectId");
        if (subjectId == null || subjectId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: subjectId");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_locate_data", "SYSTEM", "FAILED", "Missing subjectId"));
            return result;
        }

        Object dataSourcesObj = task.getInputData().get("dataSources");
        if (dataSourcesObj == null) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion(
                    "Missing required input: dataSources. Provide a list of data source maps to scan. "
                    + "Each entry must have: system (String), table (String), fields (List<String>), records (List<Map>).");
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_locate_data", subjectId, "FAILED", "Missing dataSources"));
            return result;
        }

        if (!(dataSourcesObj instanceof List)) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid input type: dataSources must be a List, got " + dataSourcesObj.getClass().getSimpleName());
            result.getOutputData().put("auditLog",
                    VerifyIdentityWorker.auditLog("gdpr_locate_data", subjectId, "FAILED", "Invalid dataSources type"));
            return result;
        }

        List<?> dataSources = (List<?>) dataSourcesObj;
        List<Map<String, Object>> locations = new ArrayList<>();
        int totalFieldsMatched = 0;

        for (Object dsObj : dataSources) {
            if (!(dsObj instanceof Map)) continue;
            Map<String, Object> ds = (Map<String, Object>) dsObj;

            String system = String.valueOf(ds.getOrDefault("system", "unknown"));
            String table = String.valueOf(ds.getOrDefault("table", "unknown"));
            List<String> fields = ds.get("fields") instanceof List ? (List<String>) ds.get("fields") : List.of();
            List<?> records = ds.get("records") instanceof List ? (List<?>) ds.get("records") : List.of();

            // Scan records for subject data
            List<String> matchedFields = new ArrayList<>();
            for (Object recObj : records) {
                if (!(recObj instanceof Map)) continue;
                Map<String, Object> record = (Map<String, Object>) recObj;
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().toString().contains(subjectId)) {
                        if (!matchedFields.contains(entry.getKey())) {
                            matchedFields.add(entry.getKey());
                        }
                    }
                }
            }

            if (!matchedFields.isEmpty()) {
                totalFieldsMatched += matchedFields.size();
                Map<String, Object> location = new LinkedHashMap<>();
                location.put("system", system);
                location.put("table", table);
                location.put("fields", fields);
                location.put("matchedFields", matchedFields);
                location.put("recordCount", records.size());
                locations.add(location);
            }
        }

        System.out.println("  [gdpr_locate] Subject " + subjectId + ": found in " + locations.size()
                + " systems, " + totalFieldsMatched + " fields matched");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dataLocations", locations);
        result.getOutputData().put("systemsScanned", dataSources.size());
        result.getOutputData().put("fieldsMatched", totalFieldsMatched);
        result.getOutputData().put("subjectId", subjectId);
        result.getOutputData().put("auditLog",
                VerifyIdentityWorker.auditLog("gdpr_locate_data", subjectId, "SUCCESS",
                        "Found data in " + locations.size() + " of " + dataSources.size() + " systems"));
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
