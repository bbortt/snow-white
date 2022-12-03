package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "test_operation")
public class TestOperation extends PanacheEntity {

    @Column(name="trace_id", length = 32, nullable = false, updatable = false, columnDefinition = "CHAR")
    public String traceId;

    @ManyToOne
    @JoinColumn(name = "test_operation_id")
    public Operation operation;

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    public TestRun testRun;
}
