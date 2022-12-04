package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "specification")
public class Specification extends PanacheEntity {

  @Column(name = "otel_service_name", length = 64, nullable = false, updatable = false)
  public String otelServiceName;

  @Column(name = "version", length = 16, nullable = false, updatable = false)
  public String version;

  @Column(name = "hash", length = 64, nullable = false, updatable = true, columnDefinition = "CHAR")
  public String sha256Hash;

  @Lob
  @Column(name = "original_file", nullable = false, updatable = true)
  public byte[] originalFile;

  @Column(name = "persisted_at")
  public OffsetDateTime persistedAt;

  @OneToMany(mappedBy = "specification")
  public Set<Operation> operations = new HashSet<>();
}
