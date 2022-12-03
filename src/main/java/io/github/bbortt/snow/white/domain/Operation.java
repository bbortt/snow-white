package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "operation")
public class Operation extends PanacheEntity {

    @Column(name = "path", length = 256, nullable = false, updatable = false)
    public String path;

    @ManyToOne
    @JoinColumn(name = "specification_id")
    public Specification specification;

    @OneToMany(mappedBy = "operation")
    public Set<TestOperation> testOperations = new HashSet<>();
}
