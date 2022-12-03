package io.github.bbortt.snow.white.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "key_value_attribute")
public class KeyValueAttribute extends PanacheEntityBase {

  @EmbeddedId
  public KeyValueAttributeKey id;

  @ManyToMany
  @JoinTable(
    name = "test_run_attribute",
    joinColumns = { @JoinColumn(name = "key"), @JoinColumn(name = "value") },
    inverseJoinColumns = { @JoinColumn(name = "test_run_id") }
  )
  public Set<TestRun> testRuns = new HashSet<>();

  @Embeddable
  class KeyValueAttributeKey implements Serializable {

    @Column(name = "key", nullable = false, updatable = false)
    public String key;

    @Column(name = "value", nullable = false, updatable = false)
    public String value;
  }
}
