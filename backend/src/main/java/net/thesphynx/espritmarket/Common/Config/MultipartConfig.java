package net.thesphynx.espritmarket.Common.Config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxFileSize = DataSize.ofMegabytes(5).toBytes();
        long maxRequestSize = DataSize.ofMegabytes(10).toBytes();
        return new MultipartConfigElement("", maxFileSize, maxRequestSize, 0);
    }

    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
