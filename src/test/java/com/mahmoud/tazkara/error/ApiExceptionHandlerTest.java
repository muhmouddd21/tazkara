package com.mahmoud.tazkara.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import static org.assertj.core.api.Assertions.assertThat;

// error/ApiExceptionHandlerTest.java — a plain unit test, no Spring
class ApiExceptionHandlerTest {
    @Test 
    void unexpectedErrorsNeverRevealTheirMessage() {
        ProblemDetail p = new ApiExceptionHandler()
            .handleUnexpected(new IllegalStateException("jdbc:postgresql://db-internal:5432 password=hunter2"));

        assertThat(p.getStatus()).isEqualTo(500);
        assertThat(p.getDetail()).doesNotContain("hunter2", "db-internal", "IllegalStateException");
        assertThat(p.getProperties()).containsKey("errorId");
    }
}