package llmfallbackchain.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FbCallClaudeWorkerTest {

    @Test
    void taskDefName() {
        String key = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            FbCallClaudeWorker worker = new FbCallClaudeWorker();
            assertEquals("fb_call_claude", worker.getTaskDefName());
        }
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, FbCallClaudeWorker::new);
        }
    }
}
