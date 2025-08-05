{{/*
Expand the name of the chart.
*/}}
{{- define "snow-white.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
In Kubernetes, name fields are constrained to 63 characters by the DNS naming spec.
The name in this configmap.yaml is constructed as follows:
   {{ include "snow-white.fullname" . }}-configmap-{{ include "snow-white.configmapChecksum" . }}
The "-configmap-" suffix adds 11 characters, and configmapChecksum takes a substring from 0 to 7 characters.

To ensure compliance with the 63-character limit, we truncate the fullname to 45 characters.
This accounts for the maximum length of the base name, "-configmap-", and the substring of "configmapChecksum".
Truncation is necessary to prevent potential DNS naming issues and ensure compatibility with Kubernetes standards.

If release name contains chart name it will be used as a full name.
*/}}
{{- define "snow-white.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 45 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 45 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 45 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels applied to all resources.
*/}}
{{- define "snow-white.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: v{{ .Chart.AppVersion }}
helm.sh/chart: snow-white
{{- end }}
{{ include "snow-white.selectorLabels" . }}
{{- end }}

{{/*
Selector labels, with static values only.
*/}}
{{- define "snow-white.selectorLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/name: {{ include "snow-white.name" . }}
app.kubernetes.io/part-of: snow-white
{{- end }}
