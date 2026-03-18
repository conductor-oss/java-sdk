package structuredoutput.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateJsonWorkerTest {

    @Test
    void taskDefName() {
        GenerateJsonWorker worker = new GenerateJsonWorker("test-key", null);
        assertEquals("so_generate_json", worker.getTaskDefName());
    }

    @Test
    void constructorThrowsWithoutApiKey() {
        String key = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            assertThrows(IllegalStateException.class, GenerateJsonWorker::new);
        }
    }
}
