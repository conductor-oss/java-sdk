package audiotranscription.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TranscribeWorkerTest {

    private final TranscribeWorker worker = new TranscribeWorker();

    @Test
    void taskDefName() {
        assertEquals("au_transcribe", worker.getTaskDefName());
    }

    @Test
    void transcribesAudio() {
        Task task = taskWith(Map.of("processedAudio", "audio_data", "language", "en-US", "speakerCount", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("transcript"));
        assertTrue(((String) result.getOutputData().get("transcript")).length() > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void producesSegments() {
        Task task = taskWith(Map.of("processedAudio", "audio_data"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> segments = (List<Map<String, Object>>) result.getOutputData().get("segments");
        assertEquals(4, segments.size());
    }

    @Test
    void countsWords() {
        Task task = taskWith(Map.of("processedAudio", "audio_data"));
        TaskResult result = worker.execute(task);

        int wordCount = (int) result.getOutputData().get("wordCount");
        assertTrue(wordCount > 0);
    }

    @Test
    void detectsSpeakers() {
        Task task = taskWith(Map.of("processedAudio", "audio_data"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("speakersDetected"));
    }

    @Test
    void transcriptContainsSpeakerLabels() {
        Task task = taskWith(Map.of("processedAudio", "audio_data"));
        TaskResult result = worker.execute(task);

        String transcript = (String) result.getOutputData().get("transcript");
        assertTrue(transcript.contains("Speaker 1"));
        assertTrue(transcript.contains("Speaker 2"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
