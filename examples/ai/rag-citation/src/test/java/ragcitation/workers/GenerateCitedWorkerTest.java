package ragcitation.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCitedWorkerTest {

    @Test
    void taskDefName() {
        GenerateCitedWorker worker = new GenerateCitedWorker("test-key", null);
        assertEquals("cr_generate_cited", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, GenerateCitedWorker::new);
        }
    }
}
