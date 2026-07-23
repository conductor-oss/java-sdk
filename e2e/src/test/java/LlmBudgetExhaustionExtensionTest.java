import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmBudgetExhaustionExtensionTest {

    @Test
    void detectsUsageLimitMessageInCauseChain() {
        Throwable failure = new AssertionError(
                "agent failed",
                new IllegalStateException("You have reached your specified API usage limits. Try again later."));

        assertTrue(LlmBudgetExhaustionExtension.isUsageLimitError(failure));
    }

    @Test
    void doesNotMaskOtherLlmFailures() {
        Throwable failure = new AssertionError("Agent failed: invalid model");

        assertFalse(LlmBudgetExhaustionExtension.isUsageLimitError(failure));
    }

    @Test
    void abortsUsageLimitFailuresAndRethrowsOtherFailures() {
        LlmBudgetExhaustionExtension extension = new LlmBudgetExhaustionExtension();
        Throwable budgetFailure =
                new AssertionError("You have reached your specified API usage limits.");
        Throwable otherFailure = new AssertionError("Agent failed: invalid model");

        assertThrows(
                TestAbortedException.class,
                () -> extension.handleTestExecutionException(null, budgetFailure));
        Throwable rethrown = assertThrows(
                Throwable.class,
                () -> extension.handleTestExecutionException(null, otherFailure));
        assertSame(otherFailure, rethrown);
    }
}
