package io.github.bbortt.snow.white.rest.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.UploadContext;

public class SimpleUploadContext implements UploadContext {

  private final String contentType;
  private final byte[] contents;

  public SimpleUploadContext(String contentTypeHeader, InputStream inputStream) throws IOException {
    this.contentType = contentTypeHeader;
    this.contents = inputStream.readAllBytes();
  }

  @Override
  public long contentLength() {
    return contents.length;
  }

  @Override
  public String getCharacterEncoding() {
    // The 'Content-Type' header may look like:
    // multipart/form-data; charset=UTF-8; boundary="xxxx"
    // in which case we can extract the charset, otherwise,
    // just default to UTF-8.
    ParameterParser parser = new ParameterParser();
    parser.setLowerCaseNames(true);
    String charset = parser.parse(contentType, ';').get("charset");
    return charset != null ? charset : StandardCharsets.UTF_8.name();
  }

  @Override
  public int getContentLength() {
    return contents.length;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(contents);
  }
}
