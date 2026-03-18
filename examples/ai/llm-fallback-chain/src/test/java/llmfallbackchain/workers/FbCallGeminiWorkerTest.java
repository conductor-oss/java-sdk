package llmfallbackchain.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FbCallGeminiWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key != null && !key.isBlank()) {
            FbCallGeminiWorker worker = new FbCallGeminiWorker();
            assertEquals("fb_call_gemini", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, FbCallGeminiWorker::new);
        }
    }
}
