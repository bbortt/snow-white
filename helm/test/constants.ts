export const defaultLogPattern =
  '%d{yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX} level="%level" thread="%thread" logger="%logger"{2}%replace(%mdc{trace_id}){^(.+)$, traceId="$1"} msg="%msg"%n%ex';
