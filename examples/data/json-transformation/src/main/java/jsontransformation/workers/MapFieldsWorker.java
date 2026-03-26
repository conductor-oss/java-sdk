package jsontransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps source fields to target fields with renaming and value transforms.
 * Input: data (map with source fields), mappingRules (list)
 * Output: mapped (transformed map), mappedFieldCount (always 6)
 *
 * Mapping rules:
 *   cust_id       -> customerId
 *   first_name + last_name -> fullName (concatenated, trimmed)
 *   email         -> emailAddress (lowercased)
 *   phone         -> phoneNumber
 *   reg_date      -> registrationDate
 *   acct_type     -> accountType (uppercased)
 */
public class MapFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jt_map_fields";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("data");
        if (data == null) {
            data = Map.of();
        }

        String firstName = data.get("first_name") != null ? data.get("first_name").toString() : "";
        String lastName = data.get("last_name") != null ? data.get("last_name").toString() : "";
        String fullName = (firstName + " " + lastName).trim();

        String email = data.get("email") != null ? data.get("email").toString().toLowerCase() : "";
        String acctType = data.get("acct_type") != null ? data.get("acct_type").toString().toUpperCase() : "STANDARD";

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("customerId", data.get("cust_id"));
        mapped.put("fullName", fullName);
        mapped.put("emailAddress", email);
        mapped.put("phoneNumber", data.get("phone"));
        mapped.put("registrationDate", data.get("reg_date"));
        mapped.put("accountType", acctType);

        System.out.println("  [jt_map_fields] Mapped " + data.size() + " source fields -> " + mapped.size() + " target fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mapped", mapped);
        result.getOutputData().put("mappedFieldCount", 6);
        return result;
    }
}
