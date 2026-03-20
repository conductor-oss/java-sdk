package ocrpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructureOutputWorkerTest {

    private final StructureOutputWorker worker = new StructureOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("oc_structure_output", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void organizesFieldsIntoSections() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("invoiceNumber", "INV-001");
        fields.put("date", "2024-03-15");
        fields.put("billTo", "Acme Corp");
        fields.put("address", "123 Main St");
        fields.put("total", "$100.00");
        fields.put("dueDate", "2024-04-15");
        fields.put("paymentTerms", "Net 30");

        Task task = taskWith(Map.of("fields", fields, "validatedText", "text", "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> structured = (Map<String, Object>) result.getOutputData().get("structured");
        assertNotNull(structured);
        assertNotNull(structured.get("header"));
        assertNotNull(structured.get("billing"));
        assertNotNull(structured.get("payment"));

        Map<String, Object> header = (Map<String, Object>) structured.get("header");
        assertEquals("INV-001", header.get("invoiceNumber"));
    }

    @Test
    void fieldCountMatchesInput() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("invoiceNumber", "INV-001");
        fields.put("date", "2024-03-15");
        fields.put("billTo", "Acme Corp");
        fields.put("address", "123 Main St");
        fields.put("total", "$100.00");
        fields.put("dueDate", "2024-04-15");
        fields.put("paymentTerms", "Net 30");

        Task task = taskWith(Map.of("fields", fields, "validatedText", "text", "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(7, result.getOutputData().get("fieldCount"));
    }

    @Test
    void handlesEmptyFields() {
        Task task = taskWith(Map.of("validatedText", "text", "documentType", "invoice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("fieldCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
