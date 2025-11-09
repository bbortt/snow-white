{{/*
Expand the name of the chart
*/}}
{{- define "snow-white.name" -}}
{{ default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
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
app.kubernetes.io/instance: {{ .context.Release.Name }}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/part-of: snow-white
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "snow-white.serviceAccountName" -}}
{{ if .Values.serviceAccount.create -}}
    {{ default (include "snow-white.name" .) .Values.serviceAccount.name }}
{{ else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
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

TODO: how can I "throw" this? { print "⚠ SECURITY WARNING: No public URL for snow-white defined!"
*/}}
{{- define "snow-white.publicHost" -}}
{{ if (empty .Values.snowWhite.ingress.host) }}
{{ fail "\n\n⚠ ERROR: You must set 'snowWhite.ingress.host' to the public URL!" }}
{{- else -}}
{{ .Values.snowWhite.ingress.host }}
{{- end -}}
{{- end -}}
