package ocrpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Organizes validated fields into a structured document.
 * Input: validatedText, fields, documentType
 * Output: structured, fieldCount
 */
public class StructureOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oc_structure_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> fields = (Map<String, Object>) task.getInputData().getOrDefault("fields", Map.of());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("invoiceNumber", fields.get("invoiceNumber"));
        header.put("date", fields.get("date"));

        Map<String, Object> billing = new LinkedHashMap<>();
        billing.put("company", fields.get("billTo"));
        billing.put("address", fields.get("address"));

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("total", fields.get("total"));
        payment.put("dueDate", fields.get("dueDate"));
        payment.put("terms", fields.get("paymentTerms"));

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("header", header);
        structured.put("billing", billing);
        structured.put("payment", payment);

        System.out.println("  [structure] Organized into structured document with " + fields.size() + " fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("structured", structured);
        result.getOutputData().put("fieldCount", fields.size());
        return result;
    }
}
