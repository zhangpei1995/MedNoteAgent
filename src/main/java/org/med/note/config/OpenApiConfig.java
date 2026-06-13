package org.med.note.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medNoteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MedNoteAgent API")
                        .description("医学说明书结构化和 Agent 在线调试接口")
                        .version("v1"))
                .servers(List.of(new Server().url("/").description("Current server")));
    }
}
