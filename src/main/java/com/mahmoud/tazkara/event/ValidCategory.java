package com.mahmoud.tazkara.event;

  import java.lang.annotation.ElementType;
  import java.lang.annotation.Retention;
  import java.lang.annotation.RetentionPolicy;
  import java.lang.annotation.Target;

  import jakarta.validation.Constraint;
  import jakarta.validation.Payload;

  @Target({ElementType.FIELD, ElementType.PARAMETER})   
  @Retention(RetentionPolicy.RUNTIME)                   
  @Constraint(validatedBy = CategoryValidator.class)    
  public @interface ValidCategory {
      String message() default "category must be one of: music, conference, culture, film, sport";
      Class<?>[] groups() default {};
      Class<? extends Payload>[] payload() default {};
  }