/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package loopfixture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.internal.CodeLoops;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lives outside the SDK's packages: the stack is read for user code only. */
class CodeLoopsTest {

    @Test
    void codeOutsideALoopIsInNone() throws Exception {
        onOwnThread(CodeLoopsTest::codeOutsideALoopIsInNoneBody);
    }

    private static void codeOutsideALoopIsInNoneBody() {
        assertTrue(CodeLoops.here().isEmpty());
    }

    @Test
    void forWhileAndDoWhileAreLoops() throws Exception {
        onOwnThread(CodeLoopsTest::forWhileAndDoWhileAreLoopsBody);
    }

    private static void forWhileAndDoWhileAreLoopsBody() {
        List<List<String>> seen = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            seen.add(CodeLoops.here());
        }
        int w = 0;
        while (w < 1) {
            seen.add(CodeLoops.here());
            w++;
        }
        int d = 0;
        do {
            seen.add(CodeLoops.here());
            d++;
        } while (d < 1);
        for (String ignored : List.of("x")) {
            seen.add(CodeLoops.here());
        }
        seen.forEach(path -> assertEquals(1, path.size(), path.toString()));
        assertEquals(4, seen.stream().distinct().count(), "each loop has its own name");
    }

    @Test
    void aGuardedPatternSwitchIsNotALoop() throws Exception {
        onOwnThread(() -> {
            // javac restarts the switch at the next case when a guard fails: a jump back that is
            // not a loop in the code
            assertEquals(List.of(List.of(), List.of()), guarded(3));
            assertEquals(List.of(List.of(), List.of()), guarded(9));
            assertEquals(List.of(List.of(), List.of()), guarded("s"));
        });
    }

    private static List<List<String>> guarded(Object value) {
        List<String> before = CodeLoops.here();
        List<String> inCase = switch (value) {
            case Integer i when i > 5 -> CodeLoops.here();
            case Integer i -> CodeLoops.here();
            default -> CodeLoops.here();
        };
        return List.of(before, inCase);
    }

    @Test
    void nestedLoopsAreListedOutermostFirst() throws Exception {
        onOwnThread(CodeLoopsTest::nestedLoopsAreListedOutermostFirstBody);
    }

    private static void nestedLoopsAreListedOutermostFirstBody() {
        List<String> outerOnly = null;
        List<String> both = null;
        for (int i = 0; i < 1; i++) {
            outerOnly = CodeLoops.here();
            for (int j = 0; j < 1; j++) {
                both = CodeLoops.here();
            }
        }
        assertEquals(1, outerOnly.size());
        assertEquals(2, both.size());
        assertEquals(outerOnly.get(0), both.get(0));
        assertNotEquals(both.get(0), both.get(1));
    }

    @Test
    void aLoopIsSeenThroughALambdaAndAHelperMethod() throws Exception {
        onOwnThread(CodeLoopsTest::aLoopIsSeenThroughALambdaAndAHelperMethodBody);
    }

    private static void aLoopIsSeenThroughALambdaAndAHelperMethodBody() {
        List<String> viaLambda = null;
        List<String> viaHelper = null;
        for (int i = 0; i < 1; i++) {
            Supplier<List<String>> body = CodeLoops::here;
            viaLambda = call(() -> CodeLoops.here());
            viaHelper = helper();
            assertEquals(1, body.get().size());
        }
        assertEquals(1, viaLambda.size());
        assertEquals(1, viaHelper.size());
    }

    @Test
    void aLoopInAHelperIsInsideTheLoopThatCalledIt() throws Exception {
        onOwnThread(CodeLoopsTest::aLoopInAHelperIsInsideTheLoopThatCalledItBody);
    }

    private static void aLoopInAHelperIsInsideTheLoopThatCalledItBody() {
        List<String> path = null;
        for (int i = 0; i < 1; i++) {
            path = helperWithLoop();
        }
        assertEquals(2, path.size());
        assertNotEquals(path.get(0), path.get(1));
    }

    /** Like a lane, on a thread whose stack holds only this code, not the test framework's loops. */
    private static void onOwnThread(Runnable body) throws Exception {
        Throwable[] failure = new Throwable[1];
        Thread thread = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        thread.start();
        thread.join();
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    private static List<String> call(Supplier<List<String>> s) {
        return s.get();
    }

    private static List<String> helper() {
        return CodeLoops.here();
    }

    private static List<String> helperWithLoop() {
        List<String> path = null;
        for (int i = 0; i < 1; i++) {
            path = CodeLoops.here();
        }
        return path;
    }
}
