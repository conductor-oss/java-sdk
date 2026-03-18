package googlegemini.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Prepares a Gemini-style request body with parts-based content structure,
 * generation config, and safety settings.
 */
public class GeminiPrepareContentWorker implements Worker {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    @Override
    public String getTaskDefName() {
        return "gemini_prepare_content";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String context = (String) task.getInputData().get("context");
        Object modelObj = task.getInputData().get("model");
        String model = (modelObj != null && !modelObj.toString().isBlank())
                ? modelObj.toString() : configuredDefaultModel();

        // Explicitly coerce numeric types — Conductor's JSON round-trip can
        // change Integer to Long or Double to BigDecimal, and the Gemini API
        // may reject malformed numeric fields.
        double temperature = ((Number) task.getInputData().get("temperature")).doubleValue();
        int topK = ((Number) task.getInputData().get("topK")).intValue();
        double topP = ((Number) task.getInputData().get("topP")).doubleValue();
        int maxOutputTokens = ((Number) task.getInputData().get("maxOutputTokens")).intValue();

        String combinedText = "Context: " + context + "\n\n" + prompt;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", combinedText)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "topK", topK,
                        "topP", topP,
                        "maxOutputTokens", maxOutputTokens
                ),
                "safetySettings", List.of(
                        Map.of("category", "HARM_CATEGORY_HARASSMENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE"),
                        Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                                "threshold", "BLOCK_MEDIUM_AND_ABOVE")
                )
        );

        System.out.println("  [prep] Gemini request: model=" + model + ", topK=" + topK);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestBody", requestBody);
        result.getOutputData().put("model", model);
        return result;
    }

    static String configuredDefaultModel() {
        String configured = System.getenv("GEMINI_MODEL");
        return configured != null && !configured.isBlank() ? configured : DEFAULT_MODEL;
    }
}
