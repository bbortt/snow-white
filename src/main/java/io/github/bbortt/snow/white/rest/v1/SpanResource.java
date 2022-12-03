package io.github.bbortt.snow.white.rest.v1;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Reactive implementation of {@link SpanResourceApi}, because the generator does not support it!
 */
@ApplicationScoped
@Path("/api/rest/v1/spans/count")
public class SpanResource {

  @GET
  @Produces({ "application/json" })
  public Uni<Long> countRecordedSpans() {
    return Uni.createFrom().item(1L);
  }
}
