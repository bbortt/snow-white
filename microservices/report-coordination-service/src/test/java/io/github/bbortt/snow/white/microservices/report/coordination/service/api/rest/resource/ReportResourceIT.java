package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class ReportResourceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/reports";

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void findReport_byCalculationId() throws Exception {
    var calculationId = UUID.fromString("3130fae9-e67c-43cd-9c2d-23aee9920736");

    qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameters(
          ReportParameters.builder()
            .serviceName("serviceName")
            .apiName("apiName")
            .lookbackWindow("1m")
            .build()
        )
        .reportStatus(PASSED)
        .build()
    );

    mockMvc
      .perform(get(ENTITY_API_URL + "/" + calculationId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.calculationId").value(calculationId.toString()))
      .andExpect(jsonPath("$.initiatedAt").value(not(nullValue())))
      .andExpect(jsonPath("$.status").value(PASSED.name()));
  }

  @Test
  void findReport_withoutRequiredCalculationId() throws Exception {
    mockMvc
      .perform(get(ENTITY_API_URL + "/not-a-uuid"))
      .andExpect(status().isBadRequest());
  }
}
