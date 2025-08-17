package org.workswap.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class LocalisationConfig implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() throws IOException {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        Path langPath = Path.of("lang");
        if (!Files.exists(langPath)) {
            return new ResourceBundleMessageSource(); // дефолт без локализаций
        }
        List<String> baseNames = new ArrayList<>();

        // рекурсивный поиск всех *_xx.properties файлов
        try (Stream<Path> paths = Files.walk(langPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().matches(".+_[a-z]{2}\\.properties"))
                .forEach(p -> {
                    String fullPath = p.toAbsolutePath().toString().replace("\\", "/");
                    String withoutExtension = fullPath.replaceAll("_[a-z]{2}\\.properties$", "");
                    baseNames.add("file:" + withoutExtension);
                });
        }

        messageSource.setBasenames(baseNames.toArray(String[]::new));
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(30);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("locale");
        resolver.setDefaultLocale(null);
        resolver.setCookieMaxAge(Duration.ofDays(30));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // Параметр в URL, например: ?lang=fi
        return interceptor;
    }

    @SuppressWarnings("null")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    public class LanguageUtils {
        public static final List<String> SUPPORTED_LANGUAGES = List.of("ru", "fi", "en", "it");
    }
}


