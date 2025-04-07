package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ObjectUtilsTest {

  @Test
  void shouldCopyNonNullFields() {
    var source = new Person();
    source.name = "Alice";
    source.age = 30;
    source.email = null;

    var target = new Person();
    target.name = "Bob";
    target.age = null;
    target.email = "bob@example.com";

    ObjectUtils.copyNonNullFields(source, target);

    assertThat(target.name).isEqualTo("Alice");
    assertThat(target.age).isEqualTo(30);
    assertThat(target.email).isEqualTo("bob@example.com");
  }

  @Test
  void shouldIgnoreNullSourceFields() {
    var source = new Person();
    source.name = null;
    source.age = null;
    source.email = null;

    var target = new Person();
    target.name = "Bob";
    target.age = 25;
    target.email = "bob@example.com";

    ObjectUtils.copyNonNullFields(source, target);

    assertThat(target.name).isEqualTo("Bob");
    assertThat(target.age).isEqualTo(25);
    assertThat(target.email).isEqualTo("bob@example.com");
  }

  @Test
  void shouldThrowOnNullInputs() {
    Person person = new Person();

    assertThatThrownBy(() -> ObjectUtils.copyNonNullFields(null, person))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Source and target must not be null!");
    assertThatThrownBy(() -> ObjectUtils.copyNonNullFields(person, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Source and target must not be null!");
  }

  @Test
  void shouldCopyInheritedFields() {
    Person.Employee source = new Person.Employee();
    source.name = "Charlie";
    source.age = 40;
    source.department = "IT";

    Person.Employee target = new Person.Employee();
    target.name = null;
    target.age = null;
    target.department = null;

    ObjectUtils.copyNonNullFields(source, target);

    assertThat(target.name).isEqualTo("Charlie");
    assertThat(target.age).isEqualTo(40);
    assertThat(target.department).isEqualTo("IT");
  }

  public static class Person {

    public String name;
    public Integer age;
    public String email;

    // For inheritance test
    static class Employee extends Person {

      public String department;
    }
  }
}
