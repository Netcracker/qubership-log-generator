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

package org.qubership.log.provider;

import org.qubership.log.monitoring.MetricsCollector;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.commons.lang3.SerializationUtils;

import java.text.DateFormat;

public class CustomMessageLogProvider extends LogProvider {

    private String message;

    private int lines;

    public CustomMessageLogProvider(DateFormat dateFormat, Boolean multiline, double multilineProbability, PrometheusMeterRegistry prometheusRegistry) {
        super(dateFormat, multiline, multilineProbability, prometheusRegistry);
        initCustomMetricsCollector(prometheusRegistry);
    }

    @Override
    protected void initCustomMetricsCollector(PrometheusMeterRegistry prometheusRegistry) {
        metricsCollector = new MetricsCollector(prometheusRegistry, "template", "customLogMessage");
    }

    @Override
    public void next() {
        System.out.println(message);
        fillMetrics(message, lines, 1);
    }

    @Override
    public String getFirstLine() {
        return null;
    }

    @Override
    public String getSecondLine() {
        return null;
    }

    protected void fillMetrics(String message, Integer linesCount, int messagesCount) {
        metricsCollector.countLines.set(linesCount);
        metricsCollector.countSymbols.set(message.length());

        metricsCollector.messagesCount.increment(1);

        int length = SerializationUtils.serialize(message).length;
        metricsCollector.messageBytes.record(length);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLinesCount(int lines) {
        this.lines = lines;
    }

    public int getLines() {
        return lines;
    }
}
