export const defaultLogPattern =
  "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} level=%level thread=\"%thread\" logger=%logger{2}%replace( traceId=%mdc{trace_id}){' traceId=$', ''} msg=\"%msg\"%n%ex";
