package llmcosttracking.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallClaudeWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            CallClaudeWorker worker = new CallClaudeWorker();
            assertEquals("ct_call_claude", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, CallClaudeWorker::new);
        }
    }
}
