package jsontransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Validates that the restructured record conforms to the expected schema.
 * Checks that identity.id and contact.email exist and are non-empty.
 * Input: record (nested map with identity, contact, account)
 * Output: validated (the record as-is), isValid (boolean)
 */
public class ValidateSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jt_validate_schema";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> record = (Map<String, Object>) task.getInputData().get("record");
        if (record == null) {
            record = Map.of();
        }

        boolean hasId = false;
        boolean hasEmail = false;

        @SuppressWarnings("unchecked")
        Map<String, Object> identity = (Map<String, Object>) record.get("identity");
        if (identity != null) {
            Object id = identity.get("id");
            hasId = id != null && !id.toString().isEmpty();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> contact = (Map<String, Object>) record.get("contact");
        if (contact != null) {
            Object email = contact.get("email");
            hasEmail = email != null && !email.toString().isEmpty();
        }

        boolean isValid = hasId && hasEmail;
        System.out.println("  [jt_validate_schema] Validation: id=" + hasId + ", email=" + hasEmail + " -> " + (isValid ? "VALID" : "INVALID"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validated", record);
        result.getOutputData().put("isValid", isValid);
        return result;
    }
}
