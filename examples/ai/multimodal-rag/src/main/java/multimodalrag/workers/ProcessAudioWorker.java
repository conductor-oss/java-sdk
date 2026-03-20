package multimodalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that processes audio references by extracting audio features.
 * Returns a list of features for each audio clip, including transcript,
 * sentiment, and speaker count.
 */
public class ProcessAudioWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mm_process_audio";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> audioRefs =
                (List<Map<String, String>>) task.getInputData().get("audioRefs");
        if (audioRefs == null) {
            audioRefs = List.of();
        }

        List<Map<String, Object>> features = List.of(
                Map.of(
                        "audioId", "aud-001",
                        "transcript", "In this presentation we discuss the benefits of multimodal retrieval "
                                + "augmented generation for enterprise search applications.",
                        "sentiment", "neutral",
                        "speakerCount", 2
                )
        );

        System.out.println("  [process_audio] Extracted features from " + audioRefs.size()
                + " audio clips -> " + features.size() + " feature sets");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("audioFeatures", features);
        return result;
    }
}
