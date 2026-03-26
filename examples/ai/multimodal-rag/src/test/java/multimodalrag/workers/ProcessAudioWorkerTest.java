package multimodalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessAudioWorkerTest {

    private final ProcessAudioWorker worker = new ProcessAudioWorker();

    @Test
    void taskDefName() {
        assertEquals("mm_process_audio", worker.getTaskDefName());
    }

    @Test
    void returnsOneFeatureSet() {
        List<Map<String, String>> audioRefs = new ArrayList<>();
        audioRefs.add(new HashMap<>(Map.of("audioId", "aud-001", "url", "https://example.com/audio.wav")));

        Task task = taskWith(new HashMap<>(Map.of("audioRefs", audioRefs)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("audioFeatures");
        assertNotNull(features);
        assertEquals(1, features.size());
    }

    @Test
    void featureContainsAudioIdTranscriptSentimentSpeakerCount() {
        Task task = taskWith(new HashMap<>(Map.of(
                "audioRefs", List.of(Map.of("audioId", "aud-001"))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("audioFeatures");
        Map<String, Object> first = features.get(0);

        assertEquals("aud-001", first.get("audioId"));
        assertNotNull(first.get("transcript"));
        assertEquals("neutral", first.get("sentiment"));
        assertEquals(2, first.get("speakerCount"));
    }

    @Test
    void transcriptContainsMeaningfulContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "audioRefs", List.of(Map.of("audioId", "aud-001"))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("audioFeatures");
        String transcript = (String) features.get(0).get("transcript");
        assertTrue(transcript.contains("multimodal"));
    }

    @Test
    void handlesNullAudioRefs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("audioFeatures"));
    }

    @Test
    void handlesEmptyAudioRefs() {
        Task task = taskWith(new HashMap<>(Map.of(
                "audioRefs", new ArrayList<>()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) result.getOutputData().get("audioFeatures");
        assertNotNull(features);
        assertEquals(1, features.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
