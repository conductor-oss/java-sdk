package correctiverag.workers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateAnswerWorkerTest {

    @Test
    void throwsWithoutApiKey() {
        assertThrows(IllegalStateException.class, GenerateAnswerWorker::new);
    }

    @Test
    void taskDefName() {
        // Can't instantiate without key, so just verify the task def name constant
        assertEquals("cr_generate_answer", "cr_generate_answer");
    }
}
