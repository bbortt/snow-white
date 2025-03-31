package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nullable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class ReportParameters {

  @Id
  @GeneratedValue(strategy = SEQUENCE)
  @Column(nullable = false, updatable = false)
  private Long id;

  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String serviceName;

  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String apiName;

  @Column(updatable = false)
  private @Nullable String apiVersion;

  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String lookbackWindow;

  @Builder.Default
  @ElementCollection
  @Column(name = "attribute_value")
  @MapKeyColumn(name = "attribute_key")
  @CollectionTable(
    name = "attribute_filters",
    joinColumns = @JoinColumn(name = "report_parameter_id")
  )
  private Map<String, String> attributeFilters = new HashMap<>();
}
