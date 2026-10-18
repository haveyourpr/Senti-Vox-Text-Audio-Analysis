package com.sentivox;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

/** Port of api/src/index.ts — routes live in com.sentivox.controller, CORS is enabled in application.properties. */
@OpenAPIDefinition(
    info = @Info(
            title = SentiVoxBase.APP_NAME,
            version = SentiVoxBase.APP_VERSION,
            description = SentiVoxBase.APP_DESCRIPTION,
            contact = @Contact(
                    name = SentiVoxBase.AUTHOR_NAME,
                    url = SentiVoxBase.AUTHOR_PORTFOLIO,
                    email = SentiVoxBase.AUTHOR_EMAIL)
    )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
