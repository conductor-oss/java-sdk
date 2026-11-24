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
package io.orkes.conductor.client.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Skips the annotated test class or method when the suite runs against plain Conductor OSS, i.e.
 * when {@code CONDUCTOR_SERVER_TYPE=oss}. For Orkes-only features that OSS does not implement.
 *
 * <p>Not a composed {@code @DisabledIfEnvironmentVariable}: a meta-annotation cannot pass a
 * per-use reason through, and the reason is the record of each gap.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnOssCondition.class)
public @interface DisabledOnOss {

    /** The specific, empirically-confirmed OSS gap, reported as the skip reason. */
    String value();
}
