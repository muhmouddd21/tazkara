package com.mahmoud.tazkara.error;

public class DomainException extends RuntimeException {
    public enum Kind { NOT_FOUND, CONFLICT, FORBIDDEN, UNAUTHENTICATED }

    private final Kind kind;
    private final String code; 

    public DomainException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() { return kind; }
    public String code() { return code; }
}
