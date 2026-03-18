package dataenrichment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches records with company data based on email domain lookup.
 * Input: records (list of record maps with email field)
 * Output: enriched (records with company data added)
 */
public class LookupCompanyWorker implements Worker {

    private static final Map<String, Map<String, String>> COMPANY_DB = Map.of(
            "acme.com", Map.of("company", "Acme Corp", "industry", "Technology", "size", "500-1000"),
            "globex.com", Map.of("company", "Globex Inc", "industry", "Finance", "size", "1000-5000"),
            "initech.com", Map.of("company", "Initech", "industry", "Consulting", "size", "100-500")
    );

    private static final Map<String, String> DEFAULT_COMPANY = Map.of(
            "company", "Unknown", "industry", "Unknown", "size", "Unknown"
    );

    @Override
    public String getTaskDefName() {
        return "dr_lookup_company";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> enrichedRecord = new LinkedHashMap<>(record);
            String email = (String) record.get("email");
            String domain = "";
            if (email != null && email.contains("@")) {
                domain = email.substring(email.indexOf("@") + 1);
            }
            Map<String, String> company = COMPANY_DB.getOrDefault(domain, DEFAULT_COMPANY);
            enrichedRecord.put("company", company);
            enriched.add(enrichedRecord);
        }

        System.out.println("  [company] Enriched " + enriched.size() + " records with company data");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", enriched);
        return result;
    }
}
