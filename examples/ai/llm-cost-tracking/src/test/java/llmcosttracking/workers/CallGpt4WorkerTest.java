package llmcosttracking.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallGpt4WorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key != null && !key.isBlank()) {
            CallGpt4Worker worker = new CallGpt4Worker();
            assertEquals("ct_call_gpt4", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, CallGpt4Worker::new);
        }
    }
}
