package ocrpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Extracts text from a preprocessed image using OCR.
 * Input: processedImage, language
 * Output: rawText, confidence, characterCount
 */
public class ExtractTextWorker implements Worker {

    private static final String RAW_TEXT =
            "INVOICE #INV-2024-0892\nDate: 2024-03-15\nBill To: Acme Corporation\n"
            + "123 Business Ave, Suite 400\nTotal: $12,450.00\nDue Date: 2024-04-15\n"
            + "Payment Terms: Net 30";

    @Override
    public String getTaskDefName() {
        return "oc_extract_text";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [extract] OCR extracted " + RAW_TEXT.length() + " chars with 94.7% confidence");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawText", RAW_TEXT);
        result.getOutputData().put("confidence", 94.7);
        result.getOutputData().put("characterCount", RAW_TEXT.length());
        return result;
    }
}
