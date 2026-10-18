package com.sentivox.controller;

import com.sentivox.SentiVoxBase;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.exceptions.HttpStatusException;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Hidden
@Controller
public class ErrorController {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<Object> notFound(HttpRequest<?> request) {
        return HttpResponse.<Object>notFound().body(SentiVoxBase.error(SentiVoxBase.MSG_INVALID_ENDPOINT));
    }

    @Error(status = HttpStatus.METHOD_NOT_ALLOWED, global = true)
    public HttpResponse<Object> methodNotAllowed(HttpRequest<?> request) {
        return HttpResponse.<Object>status(HttpStatus.METHOD_NOT_ALLOWED).body(SentiVoxBase.error(SentiVoxBase.MSG_INVALID_ENDPOINT));
    }

    @Error(global = true)
    public HttpResponse<Object> internalError(HttpRequest<?> request, Throwable e) {
        if (e instanceof HttpStatusException statusException)
            return HttpResponse.<Object>status(statusException.getStatus()).body(SentiVoxBase.error(statusException.getMessage()));

        LOG.error("Unhandled error on {} {}", request.getMethod(), request.getPath(), e);
        return HttpResponse.<Object>serverError().body(SentiVoxBase.error(SentiVoxBase.MSG_INTERNAL_ERROR));
    }
}
