{{/*
Common name-generation function
*/}}
{{- define "snow-white.name" -}}
{{ printf "%s-%s-%s" .context.Chart.Name .name .context.Release.Name | trunc 63 | trimSuffix "-" }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "snow-white.labels" -}}
app.kubernetes.io/managed-by: {{ .context.Release.Service }}
app.kubernetes.io/version: {{ .context.Values.appVersionOverride | default .context.Chart.AppVersion }}
helm.sh/chart: snow-white
{{ include "snow-white.selectorLabels" . }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "snow-white.selectorLabels" -}}
app.kubernetes.io/component: {{ .name }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/part-of: snow-white
{{- end -}}

{{/*
Mode selection:
    minimal:        setup with only one running pod per microservice, ideal for poc installations
    high-available: setup with three pods per deployment-unit, for producton environments
    autoscale:      high-available setup with additional (horizontal) autoscaling enabled
*/}}
{{- define "snow-white.replicas" -}}
{{ if eq .Values.snowWhite.mode "minimal" }}
replicas: 1
{{ else if eq .Values.snowWhite.mode "high-available" }}
replicas: 3
{{ else if ne .Values.snowWhite.mode "autoscale" }}
{{ fail "\n\n⚠ ERROR: You must set 'snowWhite.mode' to a valid value: 'minimal', 'high-available' or 'autoscale'!" }}
{{- end }}
{{- end -}}

{{/*
Helper function to construct the imagePullSecrets spec
*/}}
{{- define "snow-white.imagePullSecrets" -}}
{{- with .Values.global.imagePullSecrets -}}
imagePullSecrets:
  {{- toYaml . | nindent 2 }}
{{- end -}}
{{- end -}}

{{/*
Helper function making sure that the public domain (exposed through ingress) is defined
*/}}
{{- define "snow-white.publicHost" -}}
{{ if (empty .Values.snowWhite.ingress.host) }}
{{ fail "\n\n⚠ ERROR: You must set 'snowWhite.ingress.host' to the public URL!" }}
{{- else -}}
{{ .Values.snowWhite.ingress.host }}
{{- end -}}
{{- end -}}

{{/*
Common environment variables connecting microservices to OTEL collector service
*/}}
{{- define "snow-white.otelExporterEnvVariables" -}}
- name: 'OTEL_EXPORTER_OTLP_PROTOCOL'
  value: 'grpc'
- name: 'OTEL_EXPORTER_OTLP_ENDPOINT'
  value: 'http://{{ include "snow-white.name" (dict "name" "otel-collector" "context" .) }}.{{ include "common.names.namespace" . }}.svc.cluster.local.:grpc'
{{- end -}}
