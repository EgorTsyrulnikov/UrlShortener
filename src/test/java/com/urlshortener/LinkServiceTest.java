package com.urlshortener;

import com.urlshortener.config.AppConfig;
import com.urlshortener.model.ShortLink;
import com.urlshortener.repository.LinkRepository;
import com.urlshortener.service.LinkService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class LinkServiceTest {

    private LinkService linkService;
    private LinkRepository repository;
    private UUID userUuid;

    @BeforeEach
    void setUp() {
        repository = new LinkRepository();
        AppConfig config = new AppConfig(); // Использует дефолтные, если файла нет в classpath тестов
        linkService = new LinkService(repository, config);
        userUuid = UUID.randomUUID();
    }

    @Test
    void testCreateShortLinkUniqueForUsers() {
        String longUrl = "https://example.com";
        UUID user2 = UUID.randomUUID();

        String code1 = linkService.createShortLink(longUrl, userUuid, 5);
        String code2 = linkService.createShortLink(longUrl, user2, 5);

        Assertions.assertNotEquals(code1, code2, "Ссылки для разных пользователей (или повторные запросы) должны быть уникальны");
    }

    @Test
    void testLimitBlocking() {
        String longUrl = "https://test.com";
        // Лимит 1 переход
        String shortUrl = linkService.createShortLink(longUrl, userUuid, 1);

        Assertions.assertDoesNotThrow(() -> linkService.openLink(shortUrl), "Первый переход должен пройти успешно");

        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            linkService.openLink(shortUrl);
        }, "Второй переход должен вызвать исключение");

        Assertions.assertTrue(exception.getMessage().contains("Лимит переходов по ссылке исчерпан"));
    }

    @Test
    void testAccessControl() {
        String shortUrl = linkService.createShortLink("https://site.com", userUuid, 10);
        UUID intruderUuid = UUID.randomUUID();

        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            linkService.deleteLink(shortUrl, intruderUuid);
        });

        Assertions.assertTrue(exception.getMessage().contains("Ошибка доступа"));
    }

    @Test
    void testOwnershipEditing() {
        String shortUrl = linkService.createShortLink("https://site.com", userUuid, 5);

        Assertions.assertDoesNotThrow(() -> {
            linkService.updateLimit(shortUrl, 100, userUuid);
        });

        ShortLink link = repository.findByCode(shortUrl.replace("clck.ru/", ""));
        Assertions.assertEquals(100, link.getMaxVisits());
    }

    @Test
    void testMultipleLinksPerUser() {
        String url1 = "https://example1.com";
        String url2 = "https://example2.com";

        String short1 = linkService.createShortLink(url1, userUuid, 5);
        String short2 = linkService.createShortLink(url2, userUuid, 10);

        Assertions.assertNotEquals(short1, short2, "Разные ссылки должны иметь разные коды");
        Assertions.assertEquals(2, linkService.getUserLinks(userUuid).size(), "У пользователя должно быть 2 ссылки");
    }

    @Test
    void testLinkNotFound() {
        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            linkService.openLink("clck.ru/NOTEXIST");
        });

        Assertions.assertTrue(exception.getMessage().contains("Ссылка не найдена"));
    }

    @Test
    void testDeleteByOwner() {
        String shortUrl = linkService.createShortLink("https://test.com", userUuid, 5);

        Assertions.assertDoesNotThrow(() -> {
            linkService.deleteLink(shortUrl, userUuid);
        });

        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            linkService.openLink(shortUrl);
        });

        Assertions.assertTrue(exception.getMessage().contains("Ссылка не найдена"));
    }

    @Test
    void testCustomLimitVsDefaultLimit() {
        String shortUrl1 = linkService.createShortLink("https://test1.com", userUuid, null);
        String shortUrl2 = linkService.createShortLink("https://test2.com", userUuid, 20);

        ShortLink link1 = repository.findByCode(shortUrl1.replace("clck.ru/", ""));
        ShortLink link2 = repository.findByCode(shortUrl2.replace("clck.ru/", ""));

        Assertions.assertEquals(5, link1.getMaxVisits(), "Должен использоваться дефолтный лимит из конфига");
        Assertions.assertEquals(20, link2.getMaxVisits(), "Должен использоваться кастомный лимит");
    }

    @Test
    void testMultiUserIsolation() {
        UUID user2 = UUID.randomUUID();

        String shortUrl1 = linkService.createShortLink("https://same-url.com", userUuid, 5);
        String shortUrl2 = linkService.createShortLink("https://same-url.com", user2, 5);

        Assertions.assertEquals(1, linkService.getUserLinks(userUuid).size());
        Assertions.assertEquals(1, linkService.getUserLinks(user2).size());
        Assertions.assertNotEquals(shortUrl1, shortUrl2, "Разные пользователи должны получать разные короткие ссылки");
    }
}
