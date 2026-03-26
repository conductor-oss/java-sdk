package documentverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Worker for dv_ai_extract task -- performs AI extraction of data from a document.
 *
 * Returns a deterministic extracted data map containing:
 * - name, dateOfBirth, documentNumber, expiryDate, address
 * - confidence score of 0.92
 *
 * This is the first step in the document verification workflow. After extraction,
 * the workflow pauses at a WAIT task for human verification before storing the data.
 */
public class AiExtractWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dv_ai_extract";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dv_ai_extract] Extracting data from document...");

        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("name", "Jane Doe");
        extracted.put("dateOfBirth", "1990-05-15");
        extracted.put("documentNumber", "DOC-123456789");
        extracted.put("expiryDate", "2030-05-15");
        extracted.put("address", "123 Main St, Springfield, IL 62605");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("extracted", extracted);
        result.getOutputData().put("confidence", 0.92);

        System.out.println("  [dv_ai_extract] Extraction complete (confidence=0.92).");
        return result;
    }
}
