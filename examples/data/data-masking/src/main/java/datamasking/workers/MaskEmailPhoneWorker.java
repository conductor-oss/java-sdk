package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Masks email and phone fields in records.
 * Email: "alice@example.com" -> "a***@example.com"
 * Phone: "555-123-4567" -> "***-***-4567"
 * Input: records (list), piiFields
 * Output: records (masked), maskedCount
 */
public class MaskEmailPhoneWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mk_mask_email_phone";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());

        int maskedCount = 0;
        List<Map<String, Object>> masked = new ArrayList<>();
        for (Map<String, Object> r : records) {
            Map<String, Object> copy = new LinkedHashMap<>(r);
            if (copy.containsKey("email") && copy.get("email") != null) {
                String email = String.valueOf(copy.get("email"));
                int atIndex = email.indexOf('@');
                if (atIndex > 0) {
                    copy.put("email", email.charAt(0) + "***@" + email.substring(atIndex + 1));
                }
                maskedCount++;
            }
            if (copy.containsKey("phone") && copy.get("phone") != null) {
                String phone = String.valueOf(copy.get("phone"));
                String lastFour = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
                copy.put("phone", "***-***-" + lastFour);
                maskedCount++;
            }
            masked.add(copy);
        }

        System.out.println("  [mask-contact] Masked " + maskedCount + " email/phone fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", masked);
        result.getOutputData().put("maskedCount", maskedCount);
        return result;
    }
}
