package org.workswap.core.services;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.workswap.datasource.central.model.News;

public interface NewsService {
    List<News> findAll();
    Page<News> findAll(Pageable pageable);

    News save(News news);
    void deleteById(Long id);
    News getNewsById(Long id);

    News findNews(String param);

    // Метод для сохранения с обработкой изображения
    News save(News news, MultipartFile imageFile, boolean removeImage) throws Exception;

    // Методы для публичной части
    Page<News> getPublishedNews(int page);

    List<News> findLatestPublishedNews(int count);

    // Статистические метод
    long countAll();
    long countPublished();
    Page<News> findSimilarNews(News currentNews, Pageable pageable);

    void localizeNews(News news, Locale locale);
}
