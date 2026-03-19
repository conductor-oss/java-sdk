package firstaiworkflow.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiCallLlmWorkerTest {

    @Test
    void taskDefName() {
        // Use the test constructor to avoid requiring the env var
        AiCallLlmWorker worker = new AiCallLlmWorker("test-key", null);
        assertEquals("ai_call_llm", worker.getTaskDefName());
    }

    @Test
    void constructorRunsInSimulatedModeWithoutApiKey() {
        // The default constructor reads from env; if CONDUCTOR_OPENAI_API_KEY is not set
        // it should start in simulated mode without throwing
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            AiCallLlmWorker worker = new AiCallLlmWorker();
            assertEquals("ai_call_llm", worker.getTaskDefName());
        }
    }
}
