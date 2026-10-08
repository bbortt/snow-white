/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.quality.gate;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ContainmentForm.STRENGTH;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ContainmentForm.SUBSET;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.values;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllLines;
import static java.util.Arrays.stream;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ContainmentForm;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * A container that is not a member of this enum does not compile, and a member declared with two
 * containers fails while the enum initialises, which every test here surfaces. What neither the
 * compiler nor the initialiser catches, and is asserted instead: that the relation is a forest, that
 * the walk it exposes follows the declared containments up to a root, and that
 * {@code pages/_pages/quality-gate-criteria.md} publishes the relation the enum declares rather than
 * a second version of it.
 */
class OpenApiCoverageCriteriaUnitTest {

  private static final Path PUBLISHED_CRITERIA_PAGE = Path.of(
    "pages",
    "_pages",
    "quality-gate-criteria.md"
  );

  private static final String HEADING = "## ";
  private static final String RELATIONSHIPS_HEADING =
    "## Criteria Relationships";
  private static final String CRITERIA_HEADING = "## Available Criteria";
  private static final String TREE_FENCE = "```plaintext";
  private static final String FENCE = "```";

  private static final Pattern CRITERION_NAME = Pattern.compile("[A-Z_]+");
  private static final Pattern TREE_BRANCH = Pattern.compile(
    "^[├└]── ([A-Z_]+)$"
  );
  private static final Pattern CRITERION_SECTION = Pattern.compile(
    "^### (.+)$"
  );
  private static final Pattern PROSE_SUBSET = Pattern.compile(
    "^This is a subset of \\[(.+)]\\(#(.+)\\)\\.$"
  );
  private static final Pattern PROSE_STRENGTH = Pattern.compile(
    "^This is a stricter check than \\[(.+)]\\(#(.+)\\)\\.$"
  );
  private static final Pattern NOT_IN_AN_ANCHOR = Pattern.compile(
    "[^a-z0-9 -]"
  );

  @EnumSource
  @ParameterizedTest
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void everyCriterionReachesARootWithoutContainingItself(
    OpenApiCoverageCriteria criterion
  ) {
    var walked = new LinkedHashSet<OpenApiCoverageCriteria>();
    walked.add(criterion);

    for (
      var container = criterion.getContainedBy();
      nonNull(container);
      container = container.getContainedBy()
    ) {
      assertThat(walked.add(container))
        .as(
          "%s must not contain itself, directly or transitively, but reached %s twice",
          criterion,
          container
        )
        .isTrue();
    }
  }

  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void containingCriteriaAnswersTheFullChainOfATwoLevelCriterion() {
    assertThat(POSITIVE_RESPONSE_CODE_COVERAGE.getContainedBy()).isEqualTo(
      RESPONSE_CODE_COVERAGE
    );
    assertThat(
      POSITIVE_RESPONSE_CODE_COVERAGE.getContainingCriteria()
    ).containsExactly(RESPONSE_CODE_COVERAGE);

    assertThat(
      ERROR_RESPONSE_CODE_COVERAGE.getContainingCriteria()
    ).containsExactly(RESPONSE_CODE_COVERAGE);
  }

  /**
   * The one pair the published tree drew as containment before this relation had a definition:
   * `HTTP_METHOD_COVERAGE` judges an operation and `PATH_COVERAGE` the path item around it, so
   * neither judges the other's targets at the other's pointer. Full method coverage does imply full
   * path coverage, but declaring that implication as containment would send an operation-scoped
   * waiver up to a path item no container of it ever judged.
   */
  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void pathCoverageAndMethodCoverageContainNeitherEachOther() {
    assertThat(PATH_COVERAGE.getContainedBy()).isNull();
    assertThat(PATH_COVERAGE.getContainingCriteria()).isEmpty();

    assertThat(HTTP_METHOD_COVERAGE.getContainedBy()).isNull();
    assertThat(HTTP_METHOD_COVERAGE.getContainingCriteria()).isEmpty();
  }

  /**
   * The strength form, and the only declared edge of it: `MethodCoverageCalculator` and
   * `OperationSuccessCoverageCalculator` both emit at {@code toOperationPointer}, so the two judge
   * exactly the same targets at the same pointer and differ only in the bar - called at all, versus
   * called and answered 2xx. The edge is asserted here by name because the definition admits it on
   * grounds the three subset groups do not use, and a regression would silently take the relation
   * back to subsets only.
   *
   * <p>That the reach is one-way is pinned next door rather than here: {@code
   * pathCoverageAndMethodCoverageContainNeitherEachOther} asserts {@code HTTP_METHOD_COVERAGE}
   * reaches no container at all, which is the assertion a reversed edge would break. Asserting the
   * absence from this end would read well and prove nothing, because a root's chain is empty
   * whatever this edge says.
   */
  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void operationSuccessCoverageIsTheStricterCheckOfMethodCoverage() {
    assertThat(OPERATION_SUCCESS_COVERAGE.getContainedBy()).isEqualTo(
      HTTP_METHOD_COVERAGE
    );
    assertThat(OPERATION_SUCCESS_COVERAGE.getContainmentForm()).isEqualTo(
      STRENGTH
    );
    assertThat(
      OPERATION_SUCCESS_COVERAGE.getContainingCriteria()
    ).containsExactly(HTTP_METHOD_COVERAGE);
  }

  /**
   * The form is declared rather than inferred, and the two accessors agree about which criteria
   * have one: a container comes with a form, a root comes without. That is all this asserts - which
   * form a given edge carries is pinned by {@code
   * descriptionNamesTheSameContainerTheEnumDeclares} and by the published page, both of which fail
   * when a strength edge is declared as a subset. What this catches is the edge arriving with the
   * question unanswered, which those two cannot: a form of {@code null} on a declared container
   * would throw out of their switch rather than fail an assertion.
   */
  @EnumSource
  @ParameterizedTest
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void everyDeclaredContainmentCarriesAFormAndEveryRootCarriesNone(
    OpenApiCoverageCriteria criterion
  ) {
    if (isNull(criterion.getContainedBy())) {
      assertThat(criterion.getContainmentForm())
        .as(
          "%s is a root criterion and declares no containment form",
          criterion
        )
        .isNull();

      return;
    }

    assertThat(criterion.getContainmentForm())
      .as("%s declares a container without saying how it narrows it", criterion)
      .isNotNull();
  }

  /**
   * The shape of the chain rather than one example of it: it starts at the declared container, each
   * element is contained by the one after it, and the last element is a root. Together those pin the
   * walk's seed, its order and its end - which a single example cannot, because the published tree is
   * one level deep everywhere and a chain of one is indistinguishable from a walk that stops early.
   */
  @EnumSource
  @ParameterizedTest
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void containingCriteriaFollowsTheDeclaredContainmentsUpToARoot(
    OpenApiCoverageCriteria criterion
  ) {
    var chain = criterion.getContainingCriteria();

    if (isNull(criterion.getContainedBy())) {
      assertThat(chain).as("%s is a root criterion", criterion).isEmpty();

      return;
    }

    assertThat(chain).doesNotHaveDuplicates();

    assertThat(chain.getFirst())
      .as(
        "the chain of %s starts at a criterion other than its container",
        criterion
      )
      .isEqualTo(criterion.getContainedBy());

    for (var step = 0; step < chain.size() - 1; step++) {
      assertThat(chain.get(step).getContainedBy())
        .as(
          "%s follows %s in the chain of %s without containing it",
          chain.get(step + 1),
          chain.get(step),
          criterion
        )
        .isEqualTo(chain.get(step + 1));
    }

    assertThat(chain.getLast().getContainedBy())
      .as(
        "the chain of %s ends at %s, which is contained by something the walk left out",
        criterion,
        chain.getLast()
      )
      .isNull();
  }

  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void containingCriteriaIsEmptyForARootCriterion() {
    assertThat(RESPONSE_CODE_COVERAGE.getContainedBy()).isNull();
    assertThat(RESPONSE_CODE_COVERAGE.getContainingCriteria()).isEmpty();
  }

  @Test
  void containingCriteriaCannotBeModifiedByACaller() {
    var containingCriteria =
      ERROR_RESPONSE_CODE_COVERAGE.getContainingCriteria();

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
      () -> containingCriteria.add(RESPONSE_CODE_COVERAGE)
    );
  }

  /**
   * The waiver rule that reads this relation reaches upward only, so the one-way direction is part
   * of the declaration rather than an accident of the walk.
   */
  @Test
  void containmentReachesUpwardOnly() {
    assertThat(RESPONSE_CODE_COVERAGE.getContainingCriteria()).doesNotContain(
      ERROR_RESPONSE_CODE_COVERAGE
    );
  }

  /**
   * The description's closing sentence is the enum's own view of the declaration, and it names the
   * form as well as the container: "subset" is simply false of a {@code STRENGTH} edge, whose two
   * criteria judge the same targets. A root carries neither sentence, so a dropped declaration
   * cannot leave the claim behind in prose.
   */
  @EnumSource
  @ParameterizedTest
  void descriptionNamesTheSameContainerTheEnumDeclares(
    OpenApiCoverageCriteria criterion
  ) {
    var container = criterion.getContainedBy();

    if (isNull(container)) {
      assertThat(criterion.getDescription())
        .as(
          "%s is a root criterion and claims no containment in prose",
          criterion
        )
        .doesNotContain("subset of")
        .doesNotContain("stricter check than");

      return;
    }

    var form = criterion.getContainmentForm();

    assertThat(criterion.getDescription())
      .as(
        "%s describes a containment the enum does not declare as %s of %s",
        criterion,
        form,
        container
      )
      .endsWith(describedContainment(form, container.name()))
      .doesNotContain(describedContainment(otherThan(form), container.name()));
  }

  /**
   * The closing sentence the enum description carries for a container of the given form. Bound to
   * the form rather than written once, because the two forms claim different things and the page
   * below states each in the same words the enum does.
   */
  private static String describedContainment(
    ContainmentForm form,
    String container
  ) {
    return switch (form) {
      case SUBSET -> "This is a subset of `%s`.".formatted(container);
      case STRENGTH -> "This is a stricter check than `%s`.".formatted(
        container
      );
    };
  }

  private static ContainmentForm otherThan(ContainmentForm form) {
    return switch (form) {
      case SUBSET -> STRENGTH;
      case STRENGTH -> SUBSET;
    };
  }

  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void declaredContainmentReproducesThePublishedTree() throws IOException {
    assertThat(publishedContainment()).containsExactlyInAnyOrderEntriesOf(
      declaredContainment()
    );
  }

  /**
   * The page states the relation twice - once as the tree, once as a sentence under each criterion -
   * and both are views of the declaration. Without this, the sentences are a third version of the
   * relation and can be edited into disagreement with the suite green.
   */
  @Test
  @VerifiesArch(
    ArchTraceables.ARCH_016_CRITERIA_CONTAINMENT_DECLARED_ON_THE_ENUM
  )
  void publishedProseNamesTheSameContainmentsTheEnumDeclares()
    throws IOException {
    assertThat(publishedProseContainment()).containsExactlyInAnyOrderEntriesOf(
      declaredContainment()
    );
  }

  @Test
  void everyCriterionIsDocumentedOnThePublishedPage() throws IOException {
    var publishedLabels = publishedCriteriaLabels();

    assertThat(publishedLabels).containsExactlyInAnyOrderElementsOf(
      stream(values()).map(OpenApiCoverageCriteria::getLabel).toList()
    );
  }

  private static Map<String, String> declaredContainment() {
    var containment = new LinkedHashMap<String, String>();

    for (var criterion : values()) {
      var container = criterion.getContainedBy();

      if (nonNull(container)) {
        containment.put(criterion.name(), container.name());
      }
    }

    return containment;
  }

  private static Map<String, String> publishedContainment() throws IOException {
    var containment = new LinkedHashMap<String, String>();
    String root = null;

    for (var line : publishedTree()) {
      if (line.isBlank()) {
        root = null;
      } else if (CRITERION_NAME.matcher(line).matches()) {
        root = line;
      } else {
        var branch = TREE_BRANCH.matcher(line);

        if (!branch.matches()) {
          return fail(
            "Cannot read '%s' as a criterion or a branch of one. Extend this test alongside the tree in %s.",
            line,
            PUBLISHED_CRITERIA_PAGE
          );
        }

        if (isNull(root)) {
          return fail("Branch '%s' has no criterion above it.", line);
        }

        containment.put(branch.group(1), root);
      }
    }

    return containment;
  }

  private static List<String> publishedTree() throws IOException {
    var tree = new ArrayList<String>();
    var withinRelationships = false;
    var withinTree = false;

    for (var line : readAllLines(publishedCriteriaPage())) {
      if (line.equals(RELATIONSHIPS_HEADING)) {
        withinRelationships = true;
      } else if (
        withinRelationships && !withinTree && line.startsWith(HEADING)
      ) {
        withinRelationships = false;
      } else if (withinRelationships && line.equals(TREE_FENCE)) {
        withinTree = true;
      } else if (withinTree && line.equals(FENCE)) {
        return tree;
      } else if (withinTree) {
        tree.add(line);
      }
    }

    return fail(
      "%s publishes no criteria tree under '%s'.",
      PUBLISHED_CRITERIA_PAGE,
      RELATIONSHIPS_HEADING
    );
  }

  private static List<String> publishedCriteriaLabels() throws IOException {
    var labels = new ArrayList<String>();

    for (var line : publishedCriteriaSection()) {
      var section = CRITERION_SECTION.matcher(line);

      if (section.matches()) {
        labels.add(section.group(1));
      }
    }

    return labels;
  }

  /**
   * The containment the page states in prose, criterion name to container name. The page writes
   * labels where the enum writes names, so both sides are resolved through the enum. The sentence a
   * criterion carries also has to be the one its declared form calls for, otherwise the page could
   * publish a subset claim over a pair the enum declares to be the same targets under a stricter
   * check.
   */
  private static Map<String, String> publishedProseContainment()
    throws IOException {
    var containment = new LinkedHashMap<String, String>();
    OpenApiCoverageCriteria criterion = null;

    for (var line : publishedCriteriaSection()) {
      var section = CRITERION_SECTION.matcher(line);

      if (section.matches()) {
        criterion = criterionLabelled(section.group(1));

        continue;
      }

      for (var form : ContainmentForm.values()) {
        var prose = publishedContainmentPattern(form).matcher(line);

        if (!prose.matches()) {
          continue;
        }

        if (isNull(criterion)) {
          return fail("'%s' stands above any criterion section.", line);
        }

        var container = criterionLabelled(prose.group(1));

        assertThat(prose.group(2))
          .as(
            "the link to %s points where the page generates no anchor",
            container
          )
          .isEqualTo(anchorOf(container.getLabel()));

        assertThat(form)
          .as(
            "%s is published as %s containment of %s, which the enum does not declare that way",
            criterion,
            form,
            container
          )
          .isEqualTo(criterion.getContainmentForm());

        containment.put(criterion.name(), container.name());
      }
    }

    return containment;
  }

  private static Pattern publishedContainmentPattern(ContainmentForm form) {
    return switch (form) {
      case SUBSET -> PROSE_SUBSET;
      case STRENGTH -> PROSE_STRENGTH;
    };
  }

  private static List<String> publishedCriteriaSection() throws IOException {
    var section = new ArrayList<String>();
    var withinCriteria = false;

    for (var line : readAllLines(publishedCriteriaPage())) {
      if (line.equals(CRITERIA_HEADING)) {
        withinCriteria = true;
      } else if (withinCriteria && line.startsWith(HEADING)) {
        return section;
      } else if (withinCriteria) {
        section.add(line);
      }
    }

    if (withinCriteria) {
      return section;
    }

    return fail(
      "%s publishes no criteria sections under '%s'.",
      PUBLISHED_CRITERIA_PAGE,
      CRITERIA_HEADING
    );
  }

  /**
   * The heading anchor the page generates for a criterion: lower case, punctuation other than the
   * hyphens already in a label dropped, spaces hyphenated.
   */
  private static String anchorOf(String label) {
    return NOT_IN_AN_ANCHOR.matcher(label.toLowerCase(ROOT))
      .replaceAll("")
      .replace(' ', '-');
  }

  private static OpenApiCoverageCriteria criterionLabelled(String label) {
    return stream(values())
      .filter(criterion -> criterion.getLabel().equals(label))
      .findFirst()
      .orElseGet(() ->
        fail(
          "%s documents '%s', which is no criterion's label.",
          PUBLISHED_CRITERIA_PAGE,
          label
        )
      );
  }

  /**
   * Resolved from the repository root rather than the classpath: the page is documentation this
   * module does not package, and the test exists to catch it drifting from the declaration.
   */
  private static Path publishedCriteriaPage() {
    for (
      var candidate = Path.of("").toAbsolutePath();
      nonNull(candidate);
      candidate = candidate.getParent()
    ) {
      if (exists(candidate.resolve(".clewrc.json"))) {
        return candidate.resolve(PUBLISHED_CRITERIA_PAGE);
      }
    }

    return fail(
      "No repository root above %s - expected a directory containing .clewrc.json.",
      Path.of("").toAbsolutePath()
    );
  }
}
