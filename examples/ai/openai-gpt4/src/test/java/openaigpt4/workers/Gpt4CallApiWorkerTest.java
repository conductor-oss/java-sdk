package openaigpt4.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Gpt4CallApiWorkerTest {

    @Test
    void taskDefName() {
        // Use the test constructor to avoid requiring the env var
        Gpt4CallApiWorker worker = new Gpt4CallApiWorker(null);
        assertEquals("gpt4_call_api", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, Gpt4CallApiWorker::new);
        }
    }
}
