/*
 * Copyright 2025 Conductor Authors.
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
package com.netflix.conductor.client.automator;

import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThreadFactoryProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadFactoryProvider.class);

    public static ThreadFactory createThreadFactory(ThreadFactoryType type) {
        if (type == ThreadFactoryType.VIRTUAL) {
            return Thread.ofVirtual()
                    .name("TaskRunner-", 0)
                    .factory();
        }
        return new BasicThreadFactory.Builder()
                .namingPattern("TaskRunner %d")
                .build();
    }
}
