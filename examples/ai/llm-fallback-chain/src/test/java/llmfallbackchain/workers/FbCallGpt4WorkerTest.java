package llmfallbackchain.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FbCallGpt4WorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            FbCallGpt4Worker worker = new FbCallGpt4Worker();
            assertEquals("fb_call_gpt4", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, FbCallGpt4Worker::new);
        }
    }
}
