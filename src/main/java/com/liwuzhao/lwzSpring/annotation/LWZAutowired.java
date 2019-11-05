package com.liwuzhao.lwzSpring.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LWZAutowired {
	String value() default "";
}
