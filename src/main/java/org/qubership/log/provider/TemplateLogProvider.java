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

import org.qubership.log.model.Template;
import org.qubership.log.monitoring.MetricsCollector;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateLogProvider extends LogProvider {

    static private String variableMask = "\\$\\{[a-z_0-9]*\\}";

    private Template template;

    @Override
    protected void initCustomMetricsCollector(PrometheusMeterRegistry prometheusRegistry) {
        metricsCollector = new MetricsCollector(prometheusRegistry, "template", template.getName());
    }

    @Override
    public String getSecondLine() {
        if (template.getMultiline() != null) {
            if (currentMultilineIndex == -1) {
                randomGenerator.setSeed(new Random().nextInt(1000));
                currentMultilineIndex = randomGenerator.nextInt(template.getMultiline().size());
            }

            Template mTemplate = template.getMultiline().get(currentMultilineIndex);
            return fillTemplateWithValues(mTemplate);
        }
        currentMultilineIndex = -1;
        return "";
    }

    public TemplateLogProvider(DateFormat dateFormat, Boolean multiline, double multilineProbability,
                               PrometheusMeterRegistry prometheusRegistry, Template template) throws InterruptedException {
        super(dateFormat, multiline, multilineProbability,  prometheusRegistry);
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

    private void validateMultiline(Template template) throws InterruptedException {
        if (template.getMultiline() != null) {
            for (Template templateMultiline : template.getMultiline()) {
                validateTemplate(templateMultiline, true);
            }
        }
    }

    private void validateTemplate(Template template, boolean multiline) throws InterruptedException {
        if (!multiline) {
            if (template.getName() == null || template.getName().isEmpty()) {
                throw new InterruptedException("Name is empty in template \""
                        + template.getTemplate() + "\"");
            }
            if (template.getDateFormat() != null && !template.getDateFormat().isEmpty()) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(template.getDateFormat());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new InterruptedException("Value of parameter dateFormat parameter is not valid" +
                            " in template \"" + template.getName() + " : " + template.getTemplate() + "\"");
                }
            }
        }

        String templateStr = template.getTemplate();
        Map<String, List<String>> fields = template.getFields();
        Map<String, String> fieldMasks = new HashMap<>();
        Set<String> keysWithoutPossibleValues = new HashSet<>();
        Pattern pattern = Pattern.compile(variableMask);
        Matcher matcher = pattern.matcher(templateStr);
        while (matcher.find()) {
            String key = templateStr.substring(matcher.start() + 2, matcher.end() - 1);
            if (!fieldMasks.containsKey(key) && !keysWithoutPossibleValues.contains(key)) {
                if (!fields.containsKey(key)) {
                    keysWithoutPossibleValues.add(key);
                } else {
                    fieldMasks.put(key, new StringBuilder().append("\\$\\{").append(key).append("\\}").toString());
                }
            }
        }
        if (!keysWithoutPossibleValues.isEmpty()) {
            throw new InterruptedException("There is no possible values for variables "
                    + keysWithoutPossibleValues
                    + " in template \"" + template.getTemplate() + "\"");
        } else {
            template.setFieldMasks(fieldMasks);
        }
    }

    @Override
    public String getFirstLineWithoutDate() {
        return fillTemplateWithValues(template);
    }

    private static String fillTemplateWithValues(Template template) {
        String message = template.getTemplate();
        Map<String, List<String>> fields = template.getFields();
        for (Map.Entry<String, String> maskEntry : template.getFieldMasks().entrySet()) {
            List<String> values = fields.get(maskEntry.getKey());
            message = message.replaceAll(maskEntry.getValue(), values.get(randomGenerator.nextInt(values.size())));
        }
        return message;
    }

}
