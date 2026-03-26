package audiotranscription.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PreprocessAudioWorkerTest {

    private final PreprocessAudioWorker worker = new PreprocessAudioWorker();

    @Test
    void taskDefName() {
        assertEquals("au_preprocess_audio", worker.getTaskDefName());
    }

    @Test
    void preprocessesAudio() {
        Task task = taskWith(Map.of("audioUrl", "s3://test/audio.wav", "language", "en-US"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed_audio_data", result.getOutputData().get("processedAudio"));
        assertEquals("8m45s", result.getOutputData().get("duration"));
        assertEquals(44100, result.getOutputData().get("sampleRate"));
        assertEquals(2, result.getOutputData().get("channels"));
    }

    @Test
    void handlesDefaultUrl() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("processedAudio"));
    }

    @Test
    void outputContainsDuration() {
        Task task = taskWith(Map.of("audioUrl", "test.wav"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("duration"));
    }

    @Test
    void outputContainsSampleRate() {
        Task task = taskWith(Map.of("audioUrl", "test.wav"));
        TaskResult result = worker.execute(task);

        assertEquals(44100, result.getOutputData().get("sampleRate"));
    }

    @Test
    void outputContainsChannels() {
        Task task = taskWith(Map.of("audioUrl", "test.wav"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("channels"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
