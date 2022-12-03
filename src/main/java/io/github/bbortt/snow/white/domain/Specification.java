package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "specification")
public class Specification extends PanacheEntity {

    @Column(name = "otel_service_name", length = 64, nullable = false, updatable = false)
    public String otelServiceName;

    @Column(name = "version", length = 16, nullable = false, updatable = false)
    public String version;

    @Column(name = "hash", length = 64, nullable = false, updatable = true, columnDefinition = "CHAR")
    public String sha256Hash;

//    @Lob
//    @Column(name = "original_file", nullable = false, updatable = true)
//    public byte[] originalFile;

    @Column(name = "persisted_at", columnDefinition = "TIMESTAMP WITH TIMEZONE")
    public LocalDateTime persistedAt;

    @OneToMany(mappedBy = "specification")
    public Set<Operation> operations = new HashSet<>();
}
