package com.urlshortener.service;

import com.urlshortener.config.AppConfig;
import com.urlshortener.model.ShortLink;
import com.urlshortener.repository.LinkRepository;

import java.awt.Desktop;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class LinkService {
    private final LinkRepository repository;
    private final AppConfig config;
    private final String DOMAIN = "clck.ru/"; // Mock domain for display
    private final Random random = new Random();

    public LinkService(LinkRepository repository, AppConfig config) {
        this.repository = repository;
        this.config = config;
    }

    // 1. Создание короткой ссылки
    public String createShortLink(String originalUrl, UUID userUuid, Integer customLimit) {
        String code = generateUniqueCode();
        int limit = (customLimit != null && customLimit > 0) ? customLimit : config.getDefaultLimit();

        ShortLink link = new ShortLink(
                code,
                originalUrl,
                userUuid,
                limit,
                config.getDefaultTtlMinutes()
        );

        repository.save(link);
        return DOMAIN + code;
    }

    // 2. Генерация уникального кода (Alphanumeric)
    private String generateUniqueCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb;
        do {
            sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (repository.exists(sb.toString()));
        return sb.toString();
    }

    // 3. Переход по ссылке (Логика + Открытие браузера)
    public void openLink(String shortUrl) throws Exception {
        String code = extractCode(shortUrl);
        ShortLink link = repository.findByCode(code);

        if (link == null) {
            throw new Exception("Ошибка: Ссылка не найдена.");
        }

        if (link.isExpired()) {
            repository.delete(code); // Удаляем сразу, если наткнулись
            throw new Exception("Уведомление: Срок действия ссылки истек. Она удалена.");
        }

        if (link.isLimitReached()) {
            throw new Exception("Уведомление: Лимит переходов по ссылке исчерпан. Ссылка недоступна.");
        }

        link.incrementVisits();

        // Открытие в браузере
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(link.getOriginalUrl()));
            System.out.println("Перенаправление на: " + link.getOriginalUrl());
        } else {
            System.out.println("Не удалось открыть браузер автоматически. Перейдите по ссылке: " + link.getOriginalUrl());
        }
    }

    // 4. Администрирование (удаление)
    public void deleteLink(String shortUrl, UUID userUuid) throws Exception {
        String code = extractCode(shortUrl);
        ShortLink link = repository.findByCode(code);

        validateOwnership(link, userUuid);
        repository.delete(code);
        System.out.println("Ссылка удалена.");
    }

    // 5. Редактирование лимита
    public void updateLimit(String shortUrl, int newLimit, UUID userUuid) throws Exception {
        String code = extractCode(shortUrl);
        ShortLink link = repository.findByCode(code);

        validateOwnership(link, userUuid);
        link.setMaxVisits(newLimit);
        System.out.println("Лимит обновлен. Новый лимит: " + newLimit);
    }

    // Вспомогательный метод для проверки прав
    private void validateOwnership(ShortLink link, UUID userUuid) throws Exception {
        if (link == null) throw new Exception("Ссылка не найдена.");
        if (!link.getOwnerUuid().equals(userUuid)) {
            throw new Exception("Ошибка доступа: Вы не являетесь создателем этой ссылки.");
        }
    }

    // Очистка протухших ссылок (для фонового потока)
    public void removeExpiredLinks() {
        List<ShortLink> allLinks = repository.findAll();
        for (ShortLink link : allLinks) {
            if (link.isExpired()) {
                repository.delete(link.getShortCode());
                System.out.println("[Auto-Cleanup] Ссылка " + link.getShortCode() + " удалена по истечению срока.");
            }
        }
    }

    // Получение списка ссылок пользователя
    public List<ShortLink> getUserLinks(UUID userUuid) {
        return repository.findAll().stream()
                .filter(l -> l.getOwnerUuid().equals(userUuid))
                .collect(Collectors.toList());
    }

    private String extractCode(String url) {
        return url.replace(DOMAIN, "").replace("https://", "").replace("http://", "");
    }
}
