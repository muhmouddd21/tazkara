package com.mahmoud.tazkara.error;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.AccessDeniedException;

import javax.security.sasl.AuthenticationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice 
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_BASE = "https://tazkara.dev/problems/";

    
    @ExceptionHandler (DomainException.class)
    ProblemDetail handle(DomainException ex) {
        HttpStatus status = switch (ex.kind()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
        };
        log.info("{} {}: {}", status.value(), ex.code(), ex.getMessage());   
        return problem(status, ex.code(), ex.getMessage());
    }


    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.info("409 stale-version (JPA): {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "stale-version", "the resource was modified concurrently — reload and try again");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraint(DataIntegrityViolationException ex) {
        log.warn("409 constraint-violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "constraint-violation", "the request conflicts with existing data");
    }



    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "you are not allowed to perform this action");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleUnauthenticated(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthenticated", "a valid bearer token is required");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
            ProblemDetail body = ex.getBody();                         
            body.setType(URI.create(TYPE_BASE + "validation"));
            body.setDetail("the request has invalid fields");
            body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> Map.of("field", fe.getField(),
                                    "message", Objects.requireNonNullElse(fe.getDefaultMessage(), "invalid value")))
                    .toList());
            return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("unexpected error {}", errorId, ex);          
        ProblemDetail p = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                                  "something went wrong on our side — reference " + errorId);
        p.setProperty("errorId", errorId);
        return p;
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(URI.create(TYPE_BASE + code));
        return p;
    }
}
