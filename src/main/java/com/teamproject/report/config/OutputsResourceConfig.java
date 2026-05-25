package com.teamproject.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class OutputsResourceConfig implements WebMvcConfigurer {

    private final PipelineProperties pipelineProperties;

    public OutputsResourceConfig(PipelineProperties pipelineProperties) {
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/outputs/**")
                .addResourceLocations(resolveOutputsRoot().toUri().toString());
    }

    private Path resolveOutputsRoot() {
        Path runsRoot = Path.of(pipelineProperties.getRunsRoot()).normalize();
        Path fileName = runsRoot.getFileName();
        if (fileName != null && "runs".equals(fileName.toString()) && runsRoot.getParent() != null) {
            return runsRoot.getParent();
        }
        return runsRoot;
    }
}
