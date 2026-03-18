package llmchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Worker 4: Validates parsedData against business rules.
 * Runs 4 checks: valid_intent, valid_sentiment, products_in_catalog, reply_length.
 */
public class ChainValidateWorker implements Worker {

    private static final Set<String> VALID_INTENTS =
            Set.of("inquiry", "complaint", "return", "purchase");

    private static final Set<String> VALID_SENTIMENTS =
            Set.of("positive", "neutral", "negative");

    @Override
    public String getTaskDefName() {
        return "chain_validate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> parsedData = (Map<String, Object>) task.getInputData().get("parsedData");
        String productCatalog = (String) task.getInputData().get("productCatalog");

        Set<String> catalogProducts = Arrays.stream(productCatalog.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<Map<String, Object>> checks = new ArrayList<>();

        // Check 1: valid_intent
        String intent = (String) parsedData.get("intent");
        boolean validIntent = intent != null && VALID_INTENTS.contains(intent);
        checks.add(Map.of(
                "rule", "valid_intent",
                "passed", validIntent,
                "value", intent != null ? intent : ""
        ));

        // Check 2: valid_sentiment
        String sentiment = (String) parsedData.get("sentiment");
        boolean validSentiment = sentiment != null && VALID_SENTIMENTS.contains(sentiment);
        checks.add(Map.of(
                "rule", "valid_sentiment",
                "passed", validSentiment,
                "value", sentiment != null ? sentiment : ""
        ));

        // Check 3: products_in_catalog
        List<String> suggestedProducts = (List<String>) parsedData.get("suggestedProducts");
        boolean productsInCatalog = suggestedProducts != null
                && catalogProducts.containsAll(suggestedProducts);
        checks.add(Map.of(
                "rule", "products_in_catalog",
                "passed", productsInCatalog,
                "value", suggestedProducts != null ? suggestedProducts : List.of()
        ));

        // Check 4: reply_length
        String draftReply = (String) parsedData.get("draftReply");
        int replyLength = draftReply != null ? draftReply.length() : 0;
        boolean validReplyLength = replyLength > 50 && replyLength < 2000;
        checks.add(Map.of(
                "rule", "reply_length",
                "passed", validReplyLength,
                "value", replyLength
        ));

        boolean allChecksPassed = validIntent && validSentiment
                && productsInCatalog && validReplyLength;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedResult", parsedData);
        result.getOutputData().put("checks", checks);
        result.getOutputData().put("allChecksPassed", allChecksPassed);
        return result;
    }
}
