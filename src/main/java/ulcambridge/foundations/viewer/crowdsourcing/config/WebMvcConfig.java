package ulcambridge.foundations.viewer.crowdsourcing.config;

import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return container -> {
            TomcatEmbeddedServletContainerFactory tomcat =
                (TomcatEmbeddedServletContainerFactory) container;

            tomcat.addConnectorCustomizers(
                connector -> {
                    // Allow bodies in DELETE requests.
                    connector.setParseBodyMethods("POST,PUT,DELETE");
                }
            );
        };
    }
}
