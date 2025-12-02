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

package org.qubership.log.generator;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.log.http.ApplicationHttpServer;
import org.qubership.log.model.Config;
import org.qubership.log.model.LogFormData;
import org.qubership.log.model.Template;
import org.qubership.log.provider.CustomMessageLogProvider;
import org.qubership.log.provider.LogProvider;
import org.qubership.log.provider.TemplateLogProvider;
import org.qubership.log.provider.WordLogProvider;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusRenameFilter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created on 21.06.2018.
 */
public class Generator {
    static final String DEFAULT_MESSAGES_PER_SECOND = "1000";
    static final String DEFAULT_GENERATION_TIME = "600";
    static final String DEFAULT_LOG_REPETITIONS_NUMBER = "1";
    static final Boolean DEFAULT_MULTILINE = false;
    static final String DEFAULT_MULTILINE_PROBABILITY = "0.3";
    static final String CONFIG_FILES_PATH = "/opt/app/qubership-log-generator/etc/"; // for local run: charts/qubership-log-generator/config/
    static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    static PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    void main() throws InterruptedException, IOException {
        ApplicationHttpServer.startHTTPServer(prometheusRegistry);
        String msgPerSec = System.getenv().get("LOG_MESSAGES_PER_SECOND");
        String getTime = System.getenv().get("LOG_GENERATION_TIME");
        String multiString = System.getenv().get("LOG_MESSAGES_MULTILINE");
        String multilineProbability = System.getenv().get("LOG_MULTILINE_PROBABILITY");
        String templateNamesStr = System.getenv().get("LOG_TEMPLATES");
        String configFilePathStr = System.getenv().get("LOG_CONFIG_FILES_PATH");

        final Boolean multiline = (!"true".equalsIgnoreCase(multiString) && !"false".equalsIgnoreCase(multiString)) ?
                DEFAULT_MULTILINE : Boolean.valueOf(multiString);
        final Integer messagesPerSecGlobal = Integer.valueOf((msgPerSec == null || msgPerSec.isEmpty())
                ? DEFAULT_MESSAGES_PER_SECOND : msgPerSec);
        final Double multilineProb = Double.parseDouble((multilineProbability == null || multilineProbability.isEmpty()) ?
                DEFAULT_MULTILINE_PROBABILITY : multilineProbability);
        final String configFilePath = (configFilePathStr != null && !configFilePathStr.isEmpty())
                ? configFilePathStr : CONFIG_FILES_PATH;

        final Integer timeGlobal = Integer.valueOf((getTime == null || getTime.isEmpty()) ? DEFAULT_GENERATION_TIME : getTime);

        List<String> templateNames;
        try {
            templateNames = Arrays.asList(templateNamesStr.split(","));
            if (templateNames.size() < 1) {
                throw new Exception();
            }
        } catch (Exception _) {
            throw new InterruptedException("LOG_TEMPLATES is empty or did not set. Please, fill the parameter.");
        }

        IO.println("Configs will search in directory " + configFilePath);
        Config config = getConfig(configFilePath);
        IO.println("List of found templates: " + config);

        IO.println("Template name for run which will search in config directory: " + templateNames);
        List<Template> templates = new ArrayList<>();
        for (Template template : config.getConfig()) {
            if (templateNames.contains(template.getName())) {
                templates.add(template);
            }
        }

        if (templates.size() < 1) {
            IO.println("No templates found. Please, check that templates config exists" +
                    "and parameter LOG_TEMPLATES contains template name.");
        }

        startMonitoring();

        for (Template template : templates) {
            LogProvider logProvider;
            try {
                SimpleDateFormat dateFormat = template.getDateFormat() != null
                        ? new SimpleDateFormat(template.getDateFormat()) : DEFAULT_DATE_FORMAT;
                if (template.getTemplate() != null) {
                    logProvider = new TemplateLogProvider(dateFormat, multiline, multilineProb, prometheusRegistry, template);
                } else {
                    logProvider = new WordLogProvider(dateFormat, multiline, multilineProb, prometheusRegistry, template);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new InterruptedException("Can`t initialize log provider.");
            }

            LogProvider finalLogProvider = logProvider;
            Integer time = template.getGenerationTime() > 0
                    ? Integer.valueOf(template.getGenerationTime()) : timeGlobal;
            Integer messagesPerSec = template.getMessagesPerSec() > 0
                    ? Integer.valueOf(template.getMessagesPerSec()) : messagesPerSecGlobal;
            createLogThread(time, messagesPerSec, multiline, finalLogProvider);

        }
    }

    public static void parseAndSendMessages(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            LogFormData data = objectMapper.readValue(jsonString, LogFormData.class);
            if (data.getNumberOfRep() != null) {
                startNewLogFromEditor(data.getMessage(), data.getNumberOfRep());
            } else {
                startNewLogFromEditor(data.getMessage(), data.getGenTime(), data.getMsgPerSec());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static void startNewLogFromEditor(String message, Integer genTime, Integer msgPerSec) {
        try {
            CustomMessageLogProvider provider = initAndGetProvider(message);
            createLogThread(getValidValue(genTime, DEFAULT_GENERATION_TIME), getValidValue(msgPerSec, DEFAULT_MESSAGES_PER_SECOND), provider.getLines() > 1, provider);
        } catch (IllegalArgumentException e) {
            IO.println(e.getMessage());
        }
    }


    private static void startNewLogFromEditor(String message, Integer numberOfRep) {
        try {
            createLogThread(getValidValue(numberOfRep, DEFAULT_LOG_REPETITIONS_NUMBER), initAndGetProvider(message));
        } catch (IllegalArgumentException e) {
            IO.println(e.getMessage());
        }
    }

    private static CustomMessageLogProvider initAndGetProvider(String message) {
        if (message.isEmpty()) {
            throw new IllegalArgumentException("The log message should not be empty");
        }
        int linesCount = countLines(message);
        CustomMessageLogProvider provider = new CustomMessageLogProvider(null, linesCount > 1, 0, prometheusRegistry);
        provider.setMessage(message);
        provider.setLinesCount(linesCount);
        return provider;
    }

    private static int getValidValue(Integer value, String defaultValue) {
        if (value == null) {
            return Integer.parseInt(defaultValue);
        } else if (value < 0) {
            throw new IllegalArgumentException("Number cannot be less than 0.");
        }
        return value;
    }

    private static void createLogThread(Integer numberOfRep, LogProvider provider) {
        Thread logThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long start_time = System.currentTimeMillis();
                for (int j = 0; j < numberOfRep; j++) {
                    provider.next();
                }
                long end_time = System.currentTimeMillis();
                long result_time = end_time - start_time;
                StringBuilder sb = new StringBuilder();
                sb.append("---=============---\n");
                sb.append(numberOfRep + " messages were added by " + result_time + "").append("\n");
                IO.println(sb);
            }
        });
        logThread.start();
    }

    private static void createLogThread(Integer time, Integer messagesPerSec, Boolean multiline, LogProvider finalLogProvider) {
        Thread logThread = new Thread(new Runnable() {
            @Override
            public void run() {
                IO.println("LOG_MESSAGES_PER_SECOND = " + messagesPerSec + "\n" +
                        "LOG_GENERATION_TIME = " + time + "\n" +
                        "LOG_MESSAGES_MULTILINE = " + multiline);

                for (int j = 0; j < time; j++) {
                    long start_time = System.currentTimeMillis();
                    for (int i = 0; i < messagesPerSec; i++) {
                        finalLogProvider.next();
                    }
                    long waitFor1sec = 1000 - (System.currentTimeMillis() - start_time);

                    if (waitFor1sec > 0) {
                        try {
                            Thread.sleep(waitFor1sec);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    long end_time = System.currentTimeMillis();
                    long result_time = end_time - start_time;

                    StringBuilder sb = new StringBuilder();
                    sb.append("---=============---\n");
                    sb.append(messagesPerSec + " messages were added by " + result_time + "").append("\n");
                    sb.append("waitFor1sec = " + waitFor1sec).append("\n");
                    IO.println(sb);
                }
            }
        });
        logThread.start();
    }

    private static void startMonitoring() {
        prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmCompilationMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);

    }

    public static Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.getFileName()
                            .toString());
                }
            }
        }
        return fileList;
    }

    private static int countLines(String message){
        String[] lines = message.split("\r\n|\r|\n");
        return  lines.length;
    }


    private static Config getConfig(String path) throws IOException {
        Set<String> files = listFilesUsingDirectoryStream(path);
        Config config = new Config();
        config.setConfig(new ArrayList<>());
        for (String file : files) {
            InputStream inputStream = Files.newInputStream(Path.of(path + file));
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(Config.class, loaderOptions));
            Config fileConfig = yaml.load(inputStream);
            if (fileConfig != null && fileConfig.getConfig() != null && fileConfig.getConfig().size() > 0) {
                config.getConfig().addAll(fileConfig.getConfig());
            }
        }
        return config;
    }

}
