package com.ibank.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ProblemDetail.ProblemDetailBuilder.class)
public class ProblemDetail {

    private final URI type;
    private final String title;
    private final int status;
    private final String detail;
    private final URI instance;
    private final Instant timestamp;

    @Singular
    private final Map<String, Object> properties;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ProblemDetailBuilder {}
}