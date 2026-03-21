package jsontransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Restructures a flat mapped record into nested groups.
 * Input: mapped (flat map with customerId, fullName, emailAddress, phoneNumber, accountType, registrationDate)
 * Output: restructured { identity{id, name}, contact{email, phone}, account{type, registeredAt} }
 */
public class RestructureNestedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jt_restructure_nested";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mapped = (Map<String, Object>) task.getInputData().get("mapped");
        if (mapped == null) {
            mapped = Map.of();
        }

        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("id", mapped.get("customerId"));
        identity.put("name", mapped.get("fullName"));

        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("email", mapped.get("emailAddress"));
        contact.put("phone", mapped.get("phoneNumber"));

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("type", mapped.get("accountType"));
        account.put("registeredAt", mapped.get("registrationDate"));

        Map<String, Object> restructured = new LinkedHashMap<>();
        restructured.put("identity", identity);
        restructured.put("contact", contact);
        restructured.put("account", account);

        System.out.println("  [jt_restructure_nested] Reorganized into nested structure: identity, contact, account");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("restructured", restructured);
        return result;
    }
}
