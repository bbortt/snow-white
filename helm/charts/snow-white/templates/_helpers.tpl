{{/*
Expand the name of the chart.
*/}}
{{- define "snow-white.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to all resources.
*/}}
{{- define "snow-white.labels" -}}
app.kubernetes.io/managed-by: {{ .context.Release.Service }}
{{- if .context.Chart.AppVersion }}
app.kubernetes.io/version: {{ .context.Values.appVersionOverride | default .context.Chart.AppVersion }}
{{- end }}
helm.sh/chart: snow-white
{{ include "snow-white.selectorLabels" . }}
{{- end }}

{{/*
Selector labels, with static values only.
*/}}
{{- define "snow-white.selectorLabels" -}}
app.kubernetes.io/instance: {{ .context.Release.Name }}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/part-of: snow-white
{{- end }}

{{/*
Mode selection:
    minimal:        setup with only one running pod per microservice, ideal for poc installations
    high-available: setup with three pods per deployment-unit, for producton environments
    autoscale:      high-available setup with additional (horizontal) autoscaling enabled
*/}}
{{- define "snow-white.replicas" }}
{{ if eq .Values.snowWhite.mode "minimal" }}
replicas: 1
{{ else if eq .Values.snowWhite.mode "high-available" }}
replicas: 3
{{- end }}
{{- end }}
