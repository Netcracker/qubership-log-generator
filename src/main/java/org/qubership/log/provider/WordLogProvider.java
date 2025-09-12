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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.qubership.log.model.*;

import org.qubership.log.model.Template;
import org.qubership.log.monitoring.MetricsCollector;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;


public class WordLogProvider extends LogProvider  {

    private Template template;

    public WordLogProvider(DateFormat dateFormat, Boolean multiline, double multilineProbability, PrometheusMeterRegistry prometheusRegistry, Template template) throws InterruptedException {
        super(dateFormat, multiline, multilineProbability, prometheusRegistry);
        this.template = template;
        validateTemplate(template, false);
        if (multiline) {
            if (template.getMultiline() != null) {
                validateMultiline(template);
            } else {
                this.multiline = false;
            }
        }
        initCustomMetricsCollector(prometheusRegistry);
    }

    @Override
    protected void initCustomMetricsCollector(PrometheusMeterRegistry prometheusRegistry) {
        metricsCollector = new MetricsCollector(prometheusRegistry, "template", template.getName());
    }

    @Override
    public String getFirstLine() {
        return generateLogLine(new int[]{template.getWordLength().getFrom(), template.getWordLength().getTo()}, new int[]{template.getLogLength().getMin(), template.getLogLength().getMax()}, new int[]{template.getSymbolRange().getFrom(), template.getSymbolRange().getTo()});
    }

    @Override
    public String getSecondLine() {
        if (template.getMultiline() != null) {
            if (currentMultilineIndex == -1) {
                randomGenerator.setSeed(new Random().nextInt(1000));
                currentMultilineIndex = randomGenerator.nextInt(template.getMultiline().size());
            }
            Template mTemplate = template.getMultiline().get(currentMultilineIndex);
            return generateLogLine(new int[]{mTemplate.getWordLength().getFrom(), mTemplate.getWordLength().getTo()}, new int[]{mTemplate.getLogLength().getMin(), mTemplate.getLogLength().getMax()}, new int[]{mTemplate.getSymbolRange().getFrom(), mTemplate.getSymbolRange().getTo()});
        }
        currentMultilineIndex = -1;
        return "";
    }

    public static String generateLogLine(int[] wordlength, int[] logLength, int[] unicodeRange) {
        StringBuilder logLineBuilder = new StringBuilder();
        int finalLoglength = (logLength[1] - logLength[0] + 1) + logLength[0];
        for (int i=0; i < finalLoglength; i++) {
            String randomWords = generateWordFromUnicodeRange(wordlength, unicodeRange);
            logLineBuilder.append(randomWords);
        }
        return logLineBuilder.toString();
    }

    public static String generateWordFromUnicodeRange(int[] wordlength, int[] unicodeRange) {
        Random random = new Random();
        int wordLength = random.nextInt(wordlength[1] - wordlength[0] + 1) + wordlength[0];
        StringBuilder wordBuilder = new StringBuilder();

        for (int i = 0; i < wordLength; i++) {
            int unicodePoint = random.nextInt(unicodeRange[1] - unicodeRange[0] + 1) + unicodeRange[0];
            wordBuilder.append(Character.toChars(unicodePoint));
        }

        return wordBuilder.toString();
    }

    private void validateMultiline(Template template) throws InterruptedException {
        for (Template templateMultiline : template.getMultiline()) {
            validateTemplate(templateMultiline, true);
        }
    }

    private void validateTemplate(Template template, boolean multiline) throws InterruptedException {
        if (!multiline) {
            if (template.getName() == null || template.getName().isEmpty()) {
                throw new InterruptedException("Name is empty in template \""
                        + template.getTemplate() + "\"");
            }
        }
        if (template.getWordLength() == null || template.getWordLength().getFrom() < 1 || template.getWordLength().getTo() < 1 || template.getWordLength().getTo() < template.getWordLength().getFrom())
            setDefaultWordLengthValues(template);
        if (template.getSymbolRange() == null || template.getSymbolRange().getFrom() < 1 || template.getSymbolRange().getTo() < 1 || template.getSymbolRange().getTo() < template.getSymbolRange().getFrom())
            setDefaultSymbolRangeValues(template);
        if (template.getLogLength() == null || template.getLogLength().getMin() < 1 || template.getLogLength().getMax() < 1 || template.getLogLength().getMax() < template.getLogLength().getMin())
            setDefaultLogLengthValues(template);

    }

    public void setDefaultWordLengthValues(Template template) {
        WordLength wl = new WordLength();
        wl.setFrom(5);
        wl.setTo(10);
        template.setWordLength(wl);
    }

    public void setDefaultSymbolRangeValues(Template template) {
        SymbolRange sr = new SymbolRange();
        sr.setFrom(0x0600);
        sr.setTo(0x06FF);
        template.setSymbolRange(sr);
    }

    public void setDefaultLogLengthValues(Template template) {
        LogLength ll = new LogLength();
        ll.setMin(20);
        ll.setMax(100);
        template.setLogLength(ll);
    }
}
