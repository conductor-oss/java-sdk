package multimodalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that performs a multimodal search using text embeddings, image features,
 * and audio features. Returns ranked search results from different modalities.
 */
public class MultimodalSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mm_multimodal_search";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Double> textEmbedding = (List<Double>) task.getInputData().get("textEmbedding");
        List<Map<String, Object>> imageFeatures =
                (List<Map<String, Object>>) task.getInputData().get("imageFeatures");
        List<Map<String, Object>> audioFeatures =
                (List<Map<String, Object>>) task.getInputData().get("audioFeatures");

        int embeddingDims = (textEmbedding != null) ? textEmbedding.size() : 0;
        int imageCount = (imageFeatures != null) ? imageFeatures.size() : 0;
        int audioCount = (audioFeatures != null) ? audioFeatures.size() : 0;

        List<Map<String, Object>> searchResults = List.of(
                Map.of(
                        "resultId", "res-001",
                        "modality", "text",
                        "score", 0.95,
                        "content", "Multimodal RAG combines text, image, and audio retrieval for comprehensive answers."
                ),
                Map.of(
                        "resultId", "res-002",
                        "modality", "image",
                        "score", 0.89,
                        "content", "Workflow diagram showing parallel processing of different modalities."
                ),
                Map.of(
                        "resultId", "res-003",
                        "modality", "audio",
                        "score", 0.82,
                        "content", "Audio transcript discussing enterprise search with multimodal retrieval."
                ),
                Map.of(
                        "resultId", "res-004",
                        "modality", "text",
                        "score", 0.78,
                        "content", "Cross-modal embeddings enable unified search across data types."
                )
        );

        System.out.println("  [multimodal_search] Searched with " + embeddingDims + "-dim embedding, "
                + imageCount + " image features, " + audioCount + " audio features -> "
                + searchResults.size() + " results");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchResults", searchResults);
        return result;
    }
}
