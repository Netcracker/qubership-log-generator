{{/*
Find a log generator image in various places.
Image can be found from:
* specified by user from .Values.image
* default value
*/}}
{{- define "log-generator.image" -}}
  {{- if .Values.image -}}
    {{- printf "%s" .Values.image -}}
  {{- else -}}
    {{- printf "ghcr.io/netcracker/qubership-log-generator:main" -}}
  {{- end -}}
{{- end -}}