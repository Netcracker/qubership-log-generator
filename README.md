# qubership Log Generator

* [Qubership Log Generator](#qubership-log-generator)
  * [Description](#description)
  * [Repository structure](#repository-structure)
  * [Build](#build)
  * [Configuration](#configuration)
  * [Deploy](#deploy)
  * [Template configuration](#template-configuration)
  * [Useful links](#useful-links)

## Description

Qubership-log-generator is very useful internal tool when we want to generate high load by logs for execute performance tests.
It allows you to debug logs and check logs processing by Logging Service.
It allows generating logs by predefined templates (java, go, json logs) and configure your own pattern.
It can produce various multiline messages.
qubership-log-generator provides simple UI for printing log of your format.
It provides general application metrics and metrics of logs generation statistics.

## Repository structure

* `./charts` - helm chart qubership-log-generator
* `./charts/qubership-log-generator/config` - built-in log formats
* `./src/main/java/org/qubership/log/provider` - different kinds of log format generators
* `./src/main/java/org/qubership/log/Generator.java` - application entrypoint, reads configuration and starting log generators in threads
* `./src/main/java/org/qubership/log/http` - HTTP server entrypoint
* `./src/main/java/org/qubership/log/model` - general structures
* `./static` - static files like html page for UI

## Build

qubership-log-generator is internal tool that does not have application manifest. It can be deployed only manually with Helm. It requires Docker image.

To build Docker image run builder with parameters:

* `REPOSITORY_NAME`: `Platform.Logging/benchmark/qubership-log-generator`
* `LOCATION`: `$branch tag` (or other branch/tag)

After job is successful you can find Docker image of qubership-log-generator in table `Publications`.

## Configuration

The parameters of `values.yaml` with general parameters of application are described below. 

| Name                          | Description                                                                                                                                   | Type   | Default value                  |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|--------|--------------------------------|
| `image`                       | Docker image of qubership-log-generator.                                                                                                           | Str    |                                |
| `replicas`                    | (**Optional**) The number of replicas of qubership-log-generator.                                                                                  | Int    | 1                              |
| `monitoring.metricsEnabled`   | (**Optional**) Flag for enabling metrics collecting of qubership-log-generator.                                                                    | Bool   | true                           |
| `monitoring.dashboardEnabled` | (**Optional**) Flag for enable deploy of Grafana dashboard for qubership-log-generator.                                                            | Bool   | true                           |
| `messagesPerSec`              | (**Optional**) The number of messages per second. The value is used if the same parameter is not set in template configuration.               | Int    | 1000                           |
| `generationTime`              | (**Optional**) The number of times generation logs will happen. The value is used if the same parameter is not set in template configuration. | Int    | 600                            |
| `multiline.enabled`           | (**Optional**) Flag for generation second lines of log messages.                                                                              | Bool   | false                          |
| `multiline.probability`       | (**Optional**) Probability of multiline log messages.                                                                                         | Float  | 0.3                            |
| `templates`                   | The list of template names that will be applied.                                                                                              | Slice  |                                |
| `customConfig`                | (**Optional**) Custom configuration of your own templates. [How to configure template.](#template-configuration)                              | Yaml   |                                |
| `securityContext`             | (**Optional**) Allow to specify parametes of SecurityContext (like `runAsUser` or `fsGroup`) for run pod.                                     | Object | runAsUser: 2000, fsGroup: 2000 |

Note: One message is not equivalent of one line. One message can be multiline or one line.

## Deploy

The application can be deployed with Helm. Fill the parameters in `values.yaml`, configure your own pattern if there is
a need and just run command from `charts\qubership-log-generator`:

`helm install qubership-log-generator . --namespace <namespace_name>`

## Template configuration

If you need to generate logs of predefined types configured in [charts/qubership-log-generator/config](charts/qubership-log-generator/config)
you should fill template names in `values.yaml` in `templates`.

If you need to generate logs with custom template, you need to add a configuration in `values.yaml` in `customConfig`
the following way:

```yaml
templates:
  - "example"
  - "unicode"

customConfig:
  config:
    - name: "example"
      messagesPerSec: 100
      generationTime: 6
      dateFormat: "yyyy-MM-dd HH:mm:ss,SSS"
      template: "[${level}] it is example of log message ${field_1}"
      fields:
        level:
          - INFO
          - WARN
          - ERROR
        field_1:
          - "1"
          - "2"
          - "3"
      multiline:
        - template: "It is the ${number} line of log message."
          fields:
            number:
              - second
              - third
              - fourth
    - name: "unicode"
      messagesPerSec: 100
      generationTime: 6
      dateFormat: "yyyy-MM-dd HH:mm:ss,SSS"
      symbolRange:
        from: 0x0600
        to: 0x06FF
      wordLength:
        from: 5
        to: 10
      logLength:
        max: 100
        min: 20
      multiline:
        - symbolRange:
            from: 0x0600
            to: 0x06FF
          wordLength:
            from: 5
            to: 10
          logLength:
            max: 100
            min: 20
```

The parameters of templates are described as follows:

| Name             | Description                                                                              | Type  | Default value             |
|------------------|------------------------------------------------------------------------------------------|-------|---------------------------|
| `name`           | Name of template that is used to be applied. Redundant for second line templates.        | Str   |                           |
| `messagesPerSec` | (**Optional**) The number of messages per second for the template.                       | Int   | 1000                      |
| `generationTime` | (**Optional**) The number of times generation logs will happen for the template.         | Int   | 600                       |
| `dateFormat`     | (**Optional**) String for dateFormat of generating message.                              | Str   | "yyyy-MM-dd HH:mm:ss,SSS" |
| `template`       | Pattern with variables specified.                                                        | Str   |                           |
| `fields`         | The list of fields names and available values.                                           | Slice |                           |
| `multiline`      | (**Optional**) Template for second lines of logs. It has the same structure as Template. | Slice |                           |
| `symbolRange.from`         | Start range for unicode symbols                                           | Int |                0x0600           |
| `symbolRange.to`         | End range for unicode symbols                                           | Int |                 0x06FF          |
| `wordLength.from`         | Minimum number of characters in words                                         | Int |            5               |
| `wordLength.to`         | maximum number of characters in words                                         | Int |              10             |
| `logLength.min`         | Minimum Length of log line                                           | Int |         20                  |
| `logLength.max`         | maximum Length of log line                                           | Int |            100               |

Important: 
- Variables must be specified in `${ }` and can use `a-z_0-9` symbols.
- If you set the same names for different templates all these templates will be applied.
- For parsing `dateFormat` java.text.SimpleDateFormat is used. Be careful when you write pattern. 

## Custom log messages functionality 
To add a custom log message to the log output, you can use the page `/customLogEditorPage`.
The page contains a form with a window for entering the text of the log, as well as parameters for configuring the printing of the log message:

| Name | Description    | Type    | Default value |
|------|-----|-----|---------------|
| `numberOfRep`   | (**Optional**)  How many times to print a log message | Int    | 1             |
| `genTime`   | (**Optional**) The number of times generation logs will happen.  |Int     | 600           |
| `msgPerSec`   | (**Optional**) The number of messages per second.   |Int     | 1000          |

When entering empty configuration parameter values, default parameters are applied. It is not possible to enter an empty log message.

## Ingress configuration
To access the page for adding custom logs(`/customLogEditorPage`), you need to configure ingress in `values.yaml`.
For example:
```yaml
ingress:
  enabled: true
  name: qubership-log-generator-ingress
  className: nginx
  host: qubership-log-generator.qubership.org
  port: 8080
```

## Metrics

If you enable metrics collecting, you can collect the following metrics.

Metrics related with generated messages:

* flg_messages_lines_count - (gauge) Count of lines in log messages by template names
* flg_messages_symbols_count - (gauge)Count of symbols in log messages
* flg_messages_bytes_max - (gauge) Bytes by log messages by template names
* flg_messages_bytes_sum - (summary) Bytes by log messages by template names
* flg_messages_bytes_count - (counter) Bytes by log messages by template names
* flg_messages_count_messages_total - (counter) Count of generated log messages by template names

General metrics JVM, Garbage Collector and other java metrics:

```prometheus
# HELP flg_messages_lines_count Count of lines in log messages
# TYPE flg_messages_lines_count gauge
flg_messages_lines_count{template="go",} 1.0
# HELP flg_messages_symbols_count Count of symbols in log messages
# TYPE flg_messages_symbols_count gauge
flg_messages_symbols_count{template="go",} 126.0
# HELP jvm_gc_pause_seconds Time spent in GC pause
# TYPE jvm_gc_pause_seconds summary
jvm_gc_pause_seconds_count{action="end of minor GC",cause="G1 Evacuation Pause",} 1.0
jvm_gc_pause_seconds_sum{action="end of minor GC",cause="G1 Evacuation Pause",} 0.013
# HELP jvm_gc_pause_seconds_max Time spent in GC pause
# TYPE jvm_gc_pause_seconds_max gauge
jvm_gc_pause_seconds_max{action="end of minor GC",cause="G1 Evacuation Pause",} 0.013
# HELP jvm_threads_peak_threads The peak live thread count since the Java virtual machine started or peak was reset
# TYPE jvm_threads_peak_threads gauge
jvm_threads_peak_threads 13.0
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="nonheap",id="CodeHeap 'profiled nmethods'",} 3155968.0
jvm_memory_used_bytes{area="heap",id="G1 Survivor Space",} 4194304.0
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 0.0
jvm_memory_used_bytes{area="nonheap",id="Metaspace",} 7449416.0
jvm_memory_used_bytes{area="nonheap",id="CodeHeap 'non-nmethods'",} 1224960.0
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 1.42606336E8
jvm_memory_used_bytes{area="nonheap",id="Compressed Class Space",} 864008.0
jvm_memory_used_bytes{area="nonheap",id="CodeHeap 'non-profiled nmethods'",} 1095808.0
# HELP jvm_gc_memory_promoted_bytes_total Count of positive increases in the size of the old generation memory pool before GC to after GC
# TYPE jvm_gc_memory_promoted_bytes_total counter
jvm_gc_memory_promoted_bytes_total 0.0
# HELP jvm_compilation_time_ms_total The approximate accumulated elapsed time spent in compilation
# TYPE jvm_compilation_time_ms_total counter
jvm_compilation_time_ms_total{compiler="HotSpot 64-Bit Tiered Compilers",} 2412.0
# HELP jvm_buffer_memory_used_bytes An estimate of the memory that the Java virtual machine is using for this buffer pool
# TYPE jvm_buffer_memory_used_bytes gauge
jvm_buffer_memory_used_bytes{id="mapped",} 0.0
jvm_buffer_memory_used_bytes{id="direct",} 8192.0
# HELP jvm_buffer_total_capacity_bytes An estimate of the total capacity of the buffers in this pool
# TYPE jvm_buffer_total_capacity_bytes gauge
jvm_buffer_total_capacity_bytes{id="mapped",} 0.0
jvm_buffer_total_capacity_bytes{id="direct",} 8192.0
# HELP jvm_threads_states_threads The current number of threads having NEW state
# TYPE jvm_threads_states_threads gauge
jvm_threads_states_threads{state="runnable",} 6.0
jvm_threads_states_threads{state="blocked",} 0.0
jvm_threads_states_threads{state="waiting",} 1.0
jvm_threads_states_threads{state="timed-waiting",} 6.0
jvm_threads_states_threads{state="new",} 0.0
jvm_threads_states_threads{state="terminated",} 0.0
# HELP jvm_buffer_count_buffers An estimate of the number of buffers in the pool
# TYPE jvm_buffer_count_buffers gauge
jvm_buffer_count_buffers{id="mapped",} 0.0
jvm_buffer_count_buffers{id="direct",} 1.0
# HELP flg_messages_bytes_max Bytes by log messages
# TYPE flg_messages_bytes_max gauge
flg_messages_bytes_max{template="go",} 184.0
# HELP flg_messages_bytes Bytes by log messages
# TYPE flg_messages_bytes summary
flg_messages_bytes_count{template="go",} 14754.0
flg_messages_bytes_sum{template="go",} 2181140.0
# HELP jvm_memory_max_bytes The maximum amount of memory in bytes that can be used for memory management
# TYPE jvm_memory_max_bytes gauge
jvm_memory_max_bytes{area="nonheap",id="CodeHeap 'profiled nmethods'",} 1.22912768E8
jvm_memory_max_bytes{area="heap",id="G1 Survivor Space",} -1.0
jvm_memory_max_bytes{area="heap",id="G1 Old Gen",} 8.382316544E9
jvm_memory_max_bytes{area="nonheap",id="Metaspace",} -1.0
jvm_memory_max_bytes{area="nonheap",id="CodeHeap 'non-nmethods'",} 5832704.0
jvm_memory_max_bytes{area="heap",id="G1 Eden Space",} -1.0
jvm_memory_max_bytes{area="nonheap",id="Compressed Class Space",} 1.073741824E9
jvm_memory_max_bytes{area="nonheap",id="CodeHeap 'non-profiled nmethods'",} 1.22912768E8
# HELP jvm_threads_live_threads The current number of live threads including both daemon and non-daemon threads
# TYPE jvm_threads_live_threads gauge
jvm_threads_live_threads 13.0
# HELP jvm_threads_daemon_threads The current number of live daemon threads
# TYPE jvm_threads_daemon_threads gauge
jvm_threads_daemon_threads 7.0
# HELP jvm_gc_max_data_size_bytes Max size of long-lived heap memory pool
# TYPE jvm_gc_max_data_size_bytes gauge
jvm_gc_max_data_size_bytes 8.382316544E9
# HELP jvm_gc_live_data_size_bytes Size of long-lived heap memory pool after reclamation
# TYPE jvm_gc_live_data_size_bytes gauge
jvm_gc_live_data_size_bytes 0.0
# HELP jvm_gc_memory_allocated_bytes_total Incremented for an increase in the size of the (young) heap memory pool after one GC to before the next
# TYPE jvm_gc_memory_allocated_bytes_total counter
jvm_gc_memory_allocated_bytes_total 2.5165824E7
# HELP flg_messages_count_messages_total Count of generated log messages
# TYPE flg_messages_count_messages_total counter
flg_messages_count_messages_total{template="go",} 14764.0
# HELP jvm_memory_committed_bytes The amount of memory in bytes that is committed for the Java virtual machine to use
# TYPE jvm_memory_committed_bytes gauge
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'profiled nmethods'",} 3211264.0
jvm_memory_committed_bytes{area="heap",id="G1 Survivor Space",} 4194304.0
jvm_memory_committed_bytes{area="heap",id="G1 Old Gen",} 1.97132288E8
jvm_memory_committed_bytes{area="nonheap",id="Metaspace",} 8257536.0
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'non-nmethods'",} 2555904.0
jvm_memory_committed_bytes{area="heap",id="G1 Eden Space",} 3.27155712E8
jvm_memory_committed_bytes{area="nonheap",id="Compressed Class Space",} 1179648.0
jvm_memory_committed_bytes{area="nonheap",id="CodeHeap 'non-profiled nmethods'",} 2555904.0
```