package com.mushan.tucangbackend.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Springfox 2.x does not understand the PathPattern based handler mappings
 * contributed by Spring Boot 2.6+ and Actuator. Keep only Ant based mappings
 * before Springfox starts to prevent DocumentationPluginsBootstrapper NPE.
 */
@Configuration
public class SpringfoxCompatibilityConfig {

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }
        };
    }

    private static void customizeSpringfoxHandlerMappings(
            List<RequestMappingInfoHandlerMapping> handlerMappings) {
        if (handlerMappings == null) {
            return;
        }
        List<RequestMappingInfoHandlerMapping> antPathMappings = handlerMappings.stream()
                .filter(mapping -> mapping.getPatternParser() == null)
                .collect(Collectors.toList());
        handlerMappings.clear();
        handlerMappings.addAll(antPathMappings);
    }

    @SuppressWarnings("unchecked")
    private static List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
        Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return (List<RequestMappingInfoHandlerMapping>) ReflectionUtils.getField(field, bean);
    }
}
