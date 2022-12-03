package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "test_run")
public class TestRun extends PanacheEntity {

    @OneToMany(mappedBy = "testRun")
    public Set<TestOperation> testOperations = new HashSet<>();
}
