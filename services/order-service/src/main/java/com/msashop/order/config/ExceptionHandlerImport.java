package com.msashop.order.config;

import com.msashop.common.web.handler.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
public class ExceptionHandlerImport {
}

