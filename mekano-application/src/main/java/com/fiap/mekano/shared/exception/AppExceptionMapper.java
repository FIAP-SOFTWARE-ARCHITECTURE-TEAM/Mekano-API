/*
 * package com.fiap.mekano.shared.exception;
 * 
 * import jakarta.ws.rs.core.Response; import jakarta.ws.rs.ext.ExceptionMapper;
 * import jakarta.ws.rs.ext.Provider;
 * 
 * @Provider public class AppExceptionMapper implements
 * ExceptionMapper<AppException> {
 * 
 * @Override public Response toResponse(AppException exception) { return
 * Response.status(exception.statusCode()) .entity(new
 * ErrorResponse(exception.getMessage())) .build(); } }
 */