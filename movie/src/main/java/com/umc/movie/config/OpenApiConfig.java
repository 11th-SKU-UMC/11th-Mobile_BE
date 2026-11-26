package com.umc.movie.config;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.umc.movie.auth.LoginMember;
import com.umc.movie.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String API_ERROR = "ApiError";

    @Bean
    OpenAPI movieOpenApi() {
        return new OpenAPI()
                .info(new Info().title("영화 평점 서비스 API")
                        .description("UMC 11기 백엔드 워크북의 회원, 평점, 즐겨찾기 API입니다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas(API_ERROR, new ObjectSchema()
                                .addProperty("isSuccess", new BooleanSchema().example(false))
                                .addProperty("code", new StringSchema())
                                .addProperty("message", new StringSchema())
                                .addProperty("result", new Schema<>().typesItem("null"))));
    }

    @Bean
    OperationCustomizer documentMovieApi() {
        return (operation, handler) -> {
            ApiResponses responses = operation.getResponses();
            var parameters = Arrays.asList(handler.getMethodParameters());
            if (parameters.stream().anyMatch(parameter -> parameter.hasParameterAnnotation(LoginMember.class))) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                addError(responses, ErrorCode.AUTHENTICATION_REQUIRED);
                addError(responses, ErrorCode.INVALID_TOKEN);
                addError(responses, ErrorCode.EXPIRED_TOKEN);
            }
            boolean hasBody = parameters.stream().anyMatch(parameter -> parameter.hasParameterAnnotation(RequestBody.class));
            boolean hasNumericPath = parameters.stream().anyMatch(parameter ->
                    parameter.hasParameterAnnotation(PathVariable.class) && parameter.getParameterType() == Long.class);
            if (hasBody) {
                addError(responses, 400, "COMMON4001", "입력값을 확인해주세요.");
            }
            if (hasBody || hasNumericPath) {
                addError(responses, 400, "COMMON400", "요청 형식 또는 입력값이 올바르지 않습니다.");
            }
            ApiErrors errors = handler.getMethodAnnotation(ApiErrors.class);
            if (errors != null) {
                for (ErrorCode error : errors.value()) {
                    addError(responses, error);
                }
            }
            return operation;
        };
    }

    private static void addError(ApiResponses responses, ErrorCode error) {
        addError(responses, error.getStatus().value(), error.getCode(), error.getMessage());
    }

    private static void addError(ApiResponses responses, int status, String code, String message) {
        var response = responses.computeIfAbsent(Integer.toString(status), ignored ->
                new io.swagger.v3.oas.models.responses.ApiResponse().description("")
                        .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + API_ERROR)))));
        String detail = "`" + code + "` " + message;
        response.setDescription(response.getDescription().isEmpty() ? detail : response.getDescription() + "\n\n" + detail);
        var body = JsonNodeFactory.instance.objectNode()
                .put("isSuccess", false)
                .put("code", code)
                .put("message", message)
                .putNull("result");
        response.getContent().get(APPLICATION_JSON_VALUE).addExamples(code, new Example().summary(code).value(body));
    }
}
