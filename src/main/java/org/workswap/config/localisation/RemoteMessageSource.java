package org.workswap.config.localisation;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.workswap.config.localisation.LocalisationConfig.LanguageUtils;

import jakarta.annotation.PostConstruct;

@Component
public class RemoteMessageSource implements MessageSource {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<Locale, Properties> cache = new ConcurrentHashMap<>();

    @Value("${localization.remote-url}")
    private String remoteBaseUrl;

    @PostConstruct
    public void preload() {
        // Можно подгрузить языки при старте
        for (String lang : LanguageUtils.SUPPORTED_LANGUAGES) {
            loadLocale(Locale.of(lang));
        }
    }

    private void loadLocale(Locale locale) {
        try {
            String url = remoteBaseUrl + locale.getLanguage();
            String content = restTemplate.getForObject(url, String.class);
            Properties props = new Properties();
            try (var reader = new StringReader(content)) {
                props.load(reader);
            }
            cache.put(locale, props);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки локализации для " + locale + ": " + e.getMessage());
        }
    }

    @Override
    public String getMessage(@NonNull String code, @Nullable Object[] args, @Nullable String defaultMessage, @NonNull Locale locale) {
        return cache.getOrDefault(locale, new Properties()).getProperty(code, defaultMessage);
    }

    @Override
    @NonNull
    public String getMessage(@NonNull String code, @Nullable Object[] args, @NonNull Locale locale) throws NoSuchMessageException {
        String msg = cache.getOrDefault(locale, new Properties()).getProperty(code);
        if (msg == null) throw new NoSuchMessageException(code);
        return msg;
    }

    @Override
    @NonNull
    public String getMessage(@NonNull MessageSourceResolvable resolvable, @NonNull Locale locale) throws NoSuchMessageException {
        String[] codes = resolvable.getCodes();
        if (codes != null) {
            for (String code : codes) {
                String msg = cache.getOrDefault(locale, new Properties()).getProperty(code);
                if (msg != null) return msg;
            }
        }
        throw new NoSuchMessageException(codes != null ? Arrays.toString(codes) : "null");
    }
}
