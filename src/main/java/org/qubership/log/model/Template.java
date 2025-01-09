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

package org.qubership.log.model;

import java.util.List;
import java.util.Map;

public class Template {

    private String name;
    private int messagesPerSec;
    private int generationTime;
    private String dateFormat;

    private SymbolRange symbolRange = new SymbolRange();
    private WordLength wordLength = new WordLength();
    private LogLength logLength = new LogLength();
    private String template;
    private Map<String, String> fieldMasks;
    private Map<String, List<String>> fields;
    private List<Template> multiline;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getFieldMasks() {
        return fieldMasks;
    }

    public void setFieldMasks(Map<String, String> fieldMasks) {
        this.fieldMasks = fieldMasks;
    }

    public Map<String, List<String>> getFields() {
        return fields;
    }

    public void setFields(Map<String, List<String>> fields) {
        this.fields = fields;
    }

    public List<Template> getMultiline() {
        return multiline;
    }

    public void setMultiline(List<Template> multiline) {
        this.multiline = multiline;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMessagesPerSec() {
        return messagesPerSec;
    }

    public void setMessagesPerSec(int messagesPerSec) {
        this.messagesPerSec = messagesPerSec;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(int generationTime) {
        this.generationTime = generationTime;
    }

    @Override
    public String toString() {
        return "Template{" +
                "name='" + name + '\'' +
                ", messagesPerSec=" + messagesPerSec +
                ", generationTime=" + generationTime +
                ", dateFormat='" + dateFormat + '\'' +
                ", symbolRange='" + symbolRange + '\'' +
                ", wordLength='" + wordLength + '\'' +
                ", logLength='" + logLength  + '\'' +
                ", template='" + template + '\'' +
                ", fieldMasks='" + fieldMasks + '\'' +
                ", fields=" + fields +
                ", multiline=" + multiline +
                '}';
    }

    public WordLength getWordLength() {
        return wordLength;
    }

    public void setWordLength(WordLength wordLength) {
        this.wordLength = wordLength;
    }


    public SymbolRange getSymbolRange() {
        return symbolRange;
    }

    public void setSymbolRange(SymbolRange symbolRange) {
        this.symbolRange = symbolRange;
    }

    public LogLength getLogLength() {
        return logLength;
    }

    public void setLogLength(LogLength logLength) {
        this.logLength = logLength;
    }
}
