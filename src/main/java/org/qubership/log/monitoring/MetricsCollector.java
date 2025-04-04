/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.log.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {

    //length of message (symbols count)
    public final Gauge messageSymbols;
    public AtomicInteger countSymbols = new AtomicInteger(0);

    //length of message (lines count)
    public final Gauge messageLines;
    public AtomicInteger countLines = new AtomicInteger(0);

    //messages count per second
    public final Counter messagesCount;

    //length of message (bytes)
    public final DistributionSummary messageBytes;

    public MetricsCollector(PrometheusMeterRegistry registry, String... tags) {
        // flg = qubershiprship-log-generator
        this.messagesCount = Counter.builder("flg.messages.count")
                .baseUnit(BaseUnits.MESSAGES)
                .description("Count of generated log messages")
                .tags(tags)
                .register(registry);
        this.messageBytes = DistributionSummary.builder("flg.messages.bytes")
                .baseUnit(BaseUnits.BYTES)
                .description("Bytes by log messages")
                .tags(tags)
                .register(registry);
        this.messageLines = Gauge.builder("flg.messages.lines.count", countLines, AtomicInteger::get)
                .description("Count of lines in log messages")
                .tags(tags)
                .register(registry);
        this.messageSymbols = Gauge.builder("flg.messages.symbols.count", countSymbols, AtomicInteger::get)
                .description("Count of symbols in log messages")
                .tags(tags)
                .register(registry);
    }
}
