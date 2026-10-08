/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.findingStatus;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * What one criterion concluded about one target of the specification, and the traces that evidence
 * it.
 * The target is named by the spec pointer plus the discriminators beside it; the discriminators are
 * denormalized so that "which findings concern this operation" is a predicate rather than a scan
 * over pointer fragments.
 * <p>
 * Both this collection and its evidence load lazily on purpose. Every list-shaped read of a report
 * takes its ratio from {@code ApiTestResult.coverage}, and dragging a grandchild table into that
 * path is exactly the cost the denormalized column exists to avoid.
 */
@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
public class ApiTestFinding {

  @Id
  @With(PRIVATE)
  @SequenceGenerator(name = "api_test_finding_id_seq", allocationSize = 1)
  @GeneratedValue(strategy = SEQUENCE, generator = "api_test_finding_id_seq")
  @Column(nullable = false, updatable = false)
  private Long id;

  @NonNull
  @Column(nullable = false, updatable = false)
  private Short status;

  @NonNull
  @Size(min = 1, max = 512)
  @Column(nullable = false, updatable = false, length = 512)
  private String specPointer;

  @Nullable
  @Size(min = 1, max = 256)
  @Column(updatable = false, length = 256)
  private String httpPath;

  @Nullable
  @Size(min = 1, max = 16)
  @Column(updatable = false, length = 16)
  private String httpMethod;

  @Nullable
  @Size(min = 1, max = 16)
  @Column(updatable = false, length = 16)
  private String responseCode;

  @Nullable
  @Size(min = 1, max = 256)
  @Column(updatable = false, length = 256)
  private String parameterName;

  @Nullable
  @Size(min = 1, max = 128)
  @Column(updatable = false, length = 128)
  private String contentType;

  @NonNull
  @Builder.Default
  @ElementCollection
  @CollectionTable(
    name = "finding_evidence",
    joinColumns = @JoinColumn(name = "api_test_finding")
  )
  private final Set<FindingEvidence> evidence = new HashSet<>();

  @NonNull
  @ManyToOne(optional = false)
  @JoinColumn(
    name = "api_test_criteria",
    referencedColumnName = "api_test_criteria",
    nullable = false,
    updatable = false
  )
  @JoinColumn(
    name = "api_test",
    referencedColumnName = "api_test",
    nullable = false,
    updatable = false
  )
  private ApiTestResult apiTestResult;

  public @NonNull FindingStatus getStatus() {
    return findingStatus(status);
  }
}
