package dataenrichment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches records with computed credit scoring based on record data.
 * The credit score is computed from available fields:
 *   - Base score: 650
 *   - Has email: +30
 *   - Has company data: +40
 *   - Has geo data (known location): +30
 *   - Name length > 3 chars: +20
 *   - Has phone: +30
 *
 * Tier is computed from score: excellent (750+), good (700+), fair (650+), poor (<650)
 *
 * Input: records (list of record maps)
 * Output: enriched (records with credit data added)
 */
public class LookupCreditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_lookup_credit";
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

            // Compute credit score based on available data
            int score = 650; // base score

            // Check for email presence
            String email = record.get("email") != null ? record.get("email").toString() : "";
            if (!email.isEmpty() && email.contains("@")) {
                score += 30;
            }

            // Check for company data
            Object companyObj = record.get("company");
            if (companyObj instanceof Map) {
                Map<String, Object> company = (Map<String, Object>) companyObj;
                String companyName = company.get("company") != null ? company.get("company").toString() : "";
                if (!companyName.isEmpty() && !"Unknown".equals(companyName)) {
                    score += 40;
                }
            }

            // Check for geo data (known location)
            Object geoObj = record.get("geo");
            if (geoObj instanceof Map) {
                Map<String, Object> geo = (Map<String, Object>) geoObj;
                String city = geo.get("city") != null ? geo.get("city").toString() : "";
                if (!city.isEmpty() && !"Unknown".equals(city)) {
                    score += 30;
                }
            }

            // Check for name
            String name = record.get("name") != null ? record.get("name").toString() : "";
            if (name.length() > 3) {
                score += 20;
            }

            // Check for phone
            String phone = record.get("phone") != null ? record.get("phone").toString() : "";
            if (!phone.isEmpty()) {
                score += 30;
            }

            // Determine tier
            String tier;
            if (score >= 750) {
                tier = "excellent";
            } else if (score >= 700) {
                tier = "good";
            } else if (score >= 650) {
                tier = "fair";
            } else {
                tier = "poor";
            }

            Map<String, Object> credit = new LinkedHashMap<>();
            credit.put("score", score);
            credit.put("tier", tier);
            credit.put("lastChecked", LocalDate.now().toString());
            enrichedRecord.put("credit", credit);
            enriched.add(enrichedRecord);
        }

        System.out.println("  [credit] Enriched " + enriched.size() + " records with computed credit scores");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", enriched);
        return result;
    }
}
