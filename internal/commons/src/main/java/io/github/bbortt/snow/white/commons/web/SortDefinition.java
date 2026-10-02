/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.web;

import static java.util.Locale.ROOT;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

/**
 * What a list read's {@code sort} parameter may say, and what it means.
 *
 * <p>The vocabulary is the property names the endpoint <em>publishes</em>; each is declared
 * alongside the entity attribute that backs it, so the published name is what a caller sends and
 * the entity path is what reaches Spring Data. Resolving the raw parameter against the entity
 * instead would make the working vocabulary an undiscoverable implementation detail.
 *
 * <p>The published names are the <em>whole</em> vocabulary: an entity attribute that happens to back
 * one is not itself accepted. Honouring both would leave two spellings of one order in the wild and
 * no release at which the second stops working.
 *
 * <p>Every order this produces ends in the declared tiebreaker, which must be unique per row.
 * Without it a {@code LIMIT}/{@code OFFSET} page over rows that compare equal is not reproducible,
 * and a caller paging through sees a row twice or not at all.
 */
@NullMarked
@RealizesSw({
  SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES,
  SwTraceables.SW_037_LIST_ORDER_IS_STABLE_BY_DEFAULT,
})
public final class SortDefinition {

  private final Map<String, String> publishedToEntityPath;
  private final String publishedProperties;
  private final Sort defaultSort;
  private final List<String> tiebreakerEntityPaths;

  private SortDefinition(Builder builder) {
    this.publishedToEntityPath = Map.copyOf(builder.publishedToEntityPath);
    this.publishedProperties = String.join(
      ", ",
      new TreeSet<>(builder.publishedToEntityPath.keySet())
    );
    this.defaultSort = Sort.by(
      appendTiebreaker(builder.defaultOrders, builder.tiebreakerEntityPaths)
    );
    this.tiebreakerEntityPaths = List.copyOf(builder.tiebreakerEntityPaths);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Resolves a raw {@code sort} parameter into the order to query by.
   *
   * <p>An absent, empty or blank value means "no preference" and yields the default order — a
   * client that builds its query string unconditionally sends {@code sort=} when it has none, and
   * rejecting that would punish a caller who expressed nothing.
   *
   * @throws InvalidSortException if the value does not parse, or names a property or direction this
   *     definition does not accept
   */
  @RealizesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
  public Sort toSort(@Nullable String sort) {
    if (sort == null || sort.isBlank()) {
      return defaultSort;
    }

    String[] parts = sort.split(",", -1);
    if (parts.length != 2) {
      throw new InvalidSortException(sort, publishedProperties);
    }

    String entityPath = publishedToEntityPath.get(parts[0].trim());
    if (entityPath == null) {
      throw new InvalidSortException(sort, publishedProperties);
    }

    Sort.Direction direction = switch (parts[1].trim().toLowerCase(ROOT)) {
      case "asc" -> ASC;
      case "desc" -> DESC;
      default -> throw new InvalidSortException(sort, publishedProperties);
    };

    return Sort.by(
      appendTiebreaker(
        List.of(new Sort.Order(direction, entityPath)),
        tiebreakerEntityPaths
      )
    );
  }

  /**
   * Appends the tiebreaker, skipping any property the requested order already covers — repeating it
   * would be redundant, and a second {@code ORDER BY} term on the same column is ignored anyway.
   */
  private static List<Sort.Order> appendTiebreaker(
    List<Sort.Order> orders,
    List<String> tiebreakerEntityPaths
  ) {
    List<Sort.Order> complete = new ArrayList<>(orders);
    tiebreakerEntityPaths
      .stream()
      .filter(path ->
        orders.stream().noneMatch(o -> o.getProperty().equals(path))
      )
      .map(Sort.Order::asc)
      .forEach(complete::add);
    return complete;
  }

  @NullMarked
  public static final class Builder {

    private final Map<String, String> publishedToEntityPath =
      new LinkedHashMap<>();
    private final List<Sort.Order> defaultOrders = new ArrayList<>();
    private final List<String> tiebreakerEntityPaths = new ArrayList<>();

    private Builder() {}

    /** A published property whose name matches the entity attribute behind it. */
    public Builder sortable(String publishedName) {
      return sortable(publishedName, publishedName);
    }

    /** A published property served under a different name than the entity attribute behind it. */
    public Builder sortable(String publishedName, String entityPath) {
      publishedToEntityPath.put(publishedName, entityPath);
      return this;
    }

    /** Part of the order applied when the request carries no {@code sort}, by published name. */
    public Builder defaultOrder(
      Sort.Direction direction,
      String publishedName
    ) {
      defaultOrders.add(
        new Sort.Order(direction, requireSortable(publishedName))
      );
      return this;
    }

    /** The properties, by published name, that make every order unique per row. */
    public Builder tiebreaker(String... publishedNames) {
      for (String publishedName : publishedNames) {
        tiebreakerEntityPaths.add(requireSortable(publishedName));
      }
      return this;
    }

    public SortDefinition build() {
      if (defaultOrders.isEmpty()) {
        throw new IllegalStateException(
          "A sort definition needs a default order: an unordered list read cannot be paged reproducibly."
        );
      }
      if (tiebreakerEntityPaths.isEmpty()) {
        throw new IllegalStateException(
          "A sort definition needs a tiebreaker unique per row, or equal rows page unpredictably."
        );
      }
      return new SortDefinition(this);
    }

    /**
     * Resolves a name declared as a default or tiebreaker, so a typo there fails at startup rather
     * than reaching Spring Data as an unresolvable property on the first request.
     */
    private String requireSortable(String publishedName) {
      String entityPath = publishedToEntityPath.get(publishedName);
      if (entityPath == null) {
        throw new IllegalStateException(
          "'%s' is not a sortable property of this definition.".formatted(
            publishedName
          )
        );
      }
      return entityPath;
    }
  }
}
