package llmcosttracking.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallGeminiWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key != null && !key.isBlank()) {
            CallGeminiWorker worker = new CallGeminiWorker();
            assertEquals("ct_call_gemini", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, CallGeminiWorker::new);
        }
    }
}
