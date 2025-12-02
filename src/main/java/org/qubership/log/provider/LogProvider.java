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
import java.util.Date;
import java.util.Random;

public abstract class LogProvider {

    protected MetricsCollector metricsCollector;

    static PrometheusMeterRegistry prometheusMeterRegistry;

    protected static final Random randomGenerator = new Random();
    int currentMultilineIndex = -1;
    double multilineProbability;
    Boolean multiline;
    DateFormat dateFormat;


    public LogProvider(DateFormat dateFormat, Boolean multiline, double multilineProbability, PrometheusMeterRegistry prometheusRegistry) {
        this.dateFormat = dateFormat;
        this.multiline = multiline;
        this.multilineProbability = multilineProbability;
        LogProvider.prometheusMeterRegistry = prometheusRegistry;
    }

    protected abstract void initCustomMetricsCollector(PrometheusMeterRegistry prometheusRegistry);

    public void next() {
        StringBuilder message = new StringBuilder();
        Integer linesCount = 1;

        String firstLine = getFirstLine();
        message.append(firstLine);

        while (multiline && isEventHappen(multilineProbability)) {
            String secondLine = getSecondLine();
            message.append("\n").append(secondLine);
            linesCount++;
        }
        IO.println(message);
        currentMultilineIndex = -1;

        fillMetrics(message, linesCount, 1);
    }

    protected void fillMetrics(StringBuilder message, Integer linesCount, int messagesCount) {
        String msg = message.toString();

        metricsCollector.countLines.set(linesCount);
        metricsCollector.countSymbols.set(message.length());

        metricsCollector.messagesCount.increment(1);

        int length = SerializationUtils.serialize(msg).length;
        metricsCollector.messageBytes.record(length);
    }

    abstract public String getFirstLine();

    abstract public String getSecondLine();

    private static boolean isEventHappen(double probability) {
        double mm = (double) randomGenerator.nextInt(101) / 100;
        return mm <= probability;
    }
}
