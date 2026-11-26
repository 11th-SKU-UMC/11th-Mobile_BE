package com.umc.movie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void swaggerUiAndOpenApiArePublic() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
    }

    @Test
    void allFifteenOperationsHaveSummariesAndOnlyProtectedOperationsRequireBearerAuth() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andReturn().getResponse().getContentAsString();
        Set<String> publicOperations = Set.of("post /members", "post /auth/login",
                "get /members/nickname/{nickname}", "get /members/email/{email}", "get /members/{memberId}/ratings");

        int operationCount = 0;
        for (var path : mapper.readTree(json).path("paths").properties()) {
            for (var method : path.getValue().properties()) {
                operationCount++;
                String operationName = method.getKey() + " " + path.getKey();
                JsonNode operation = method.getValue();
                assertThat(operation.path("summary").asText()).as(operationName).isNotBlank();
                assertThat(operation.path("description").asText()).as(operationName).isNotBlank();
                if (publicOperations.contains(operationName)) {
                    assertThat(operation.path("security").isEmpty()).as(operationName).isTrue();
                } else {
                    assertThat(operation.at("/security/0/bearerAuth").isArray()).as(operationName).isTrue();
                    for (JsonNode parameter : operation.path("parameters")) {
                        assertThat(parameter.path("name").asText()).isNotEqualTo("memberId");
                    }
                }
            }
        }
        assertThat(operationCount).isEqualTo(15);
    }

    @Test
    void openApiIncludesFavoriteExamplesAndRatingErrorCodes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['201'].content['application/json'].schema").exists())
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['201'].content['application/json']"
                        + ".example.code").value("COMMON201"))
                .andExpect(jsonPath("$.components.schemas.CreateFavoriteRequest.properties.movieId.example").value(550))
                .andExpect(jsonPath("$.components.schemas.SignUpRequest.properties.email.example")
                        .value("challenger@example.com"))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.example").value("MoviePass!9"))
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['401'].content['application/json']"
                        + ".examples.AUTH401_2.value.isSuccess").value(false))
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['409'].content['application/json']"
                        + ".examples.FAVORITE409_1.value.code").value("FAVORITE409_1"))
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['409'].content['application/json']"
                        + ".examples.FAVORITE409_1.value.result").hasJsonPath())
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['409'].content['application/json']"
                        + ".examples.FAVORITE409_1.value.result").value(nullValue()))
                .andExpect(jsonPath("$.paths['/ratings'].post.responses['409'].content['application/json']"
                        + ".examples.RATING409_1.value.code").value("RATING409_1"))
                .andExpect(jsonPath("$.paths['/ratings/{ratingId}'].patch.responses['403'].content['application/json']"
                        + ".examples.RATING403_1.value.code").value("RATING403_1"))
                .andExpect(jsonPath("$.paths['/ratings/{ratingId}'].delete.responses['403'].content['application/json']"
                        + ".examples.RATING403_1.value.code").value("RATING403_1"));
    }
}
