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
package org.conductoross.conductor.sdk;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * Serializable functional interfaces for method references. A method reference such as {@code
 * KitchenSinkTasks::task1} passed where one of these is expected can be resolved, through {@code
 * SerializedLambda}, to the method it names, so the SDK reads the task name from the annotation
 * and the parameter and return types from the method. Nothing depends on generic type arguments
 * surviving compilation.
 */
public final class Functions {

    private Functions() {}

    public interface Fn0<T, R> extends Serializable {
        R apply(T target);
    }

    public interface Fn1<T, A, R> extends Serializable {
        R apply(T target, A a);
    }

    /** A task method of a stub, by bound reference, such as {@code tasks::labelItem}. */
    public interface TaskOf<A, R> extends Serializable {
        R apply(A a);
    }

    public interface Fn2<T, A, B, R> extends Serializable {
        R apply(T target, A a, B b);
    }

    public interface Fn3<T, A, B, C, R> extends Serializable {
        R apply(T target, A a, B b, C c);
    }

    public interface Fn4<T, A, B, C, D, R> extends Serializable {
        R apply(T target, A a, B b, C c, D d);
    }

    public interface Proc0<T> extends Serializable {
        void apply(T target);
    }

    public interface Proc1<T, A> extends Serializable {
        void apply(T target, A a);
    }

    public interface Proc2<T, A, B> extends Serializable {
        void apply(T target, A a, B b);
    }

    public interface Proc3<T, A, B, C> extends Serializable {
        void apply(T target, A a, B b, C c);
    }

    public interface Proc4<T, A, B, C, D> extends Serializable {
        void apply(T target, A a, B b, C c, D d);
    }

    /** A condition given as a method reference to a boolean property, e.g. {@code JobStatus::done}. */
    public interface Pred<T> extends Predicate<T>, Serializable {}

    /** The body of a switch case. Serializable so that its compiled code can be located. */
    public interface CaseBody<R> extends java.util.function.Supplier<R>, Serializable {}

    /** A switch case body with no result. */
    public interface CaseAction extends Runnable, Serializable {}
}
