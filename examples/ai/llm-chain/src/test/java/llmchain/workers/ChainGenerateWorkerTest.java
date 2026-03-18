package llmchain.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChainGenerateWorkerTest {

    @Test
    void taskDefName() {
        ChainGenerateWorker worker = new ChainGenerateWorker("test-key", null);
        assertEquals("chain_generate", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, ChainGenerateWorker::new);
        }
    }
}
