package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "test_run")
public class TestRun extends PanacheEntity {

  @ManyToMany
  @JoinTable(
    name = "test_run_attribute",
    joinColumns = { @JoinColumn(name = "test_run_id") },
    inverseJoinColumns = { @JoinColumn(name = "key"), @JoinColumn(name = "value") }
  )
  public Set<KeyValueAttribute> identifiedBy = new HashSet<>();
}
