package com.urlshortener;

import com.urlshortener.config.AppConfig;
import com.urlshortener.model.ShortLink;
import com.urlshortener.repository.LinkRepository;
import com.urlshortener.service.LinkService;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static UUID currentUserUuid;

    public static void main(String[] args) {
        // Инициализация
        AppConfig config = new AppConfig();
        LinkRepository repository = new LinkRepository();
        LinkService service = new LinkService(repository, config);

        // Генерация UUID пользователя (эмуляция сессии)
        currentUserUuid = UUID.randomUUID();

        // Запуск фонового очистителя (Auto-cleanup)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(service::removeExpiredLinks,
                config.getCleanupIntervalSeconds(),
                config.getCleanupIntervalSeconds(),
                TimeUnit.SECONDS);

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Сервис сокращения ссылок ===");
        System.out.println("Ваш ID: " + currentUserUuid);
        printHelp();

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "shorten":
                        if (parts.length < 2) {
                            System.out.println("Использование: shorten <url> [limit]");
                            break;
                        }
                        String url = parts[1];
                        Integer limit = parts.length > 2 ? Integer.parseInt(parts[2]) : null;
                        String shortUrl = service.createShortLink(url, currentUserUuid, limit);
                        System.out.println("Короткая ссылка: " + shortUrl);
                        break;

                    case "open":
                        if (parts.length < 2) {
                            System.out.println("Использование: open <short_code_or_url>");
                            break;
                        }
                        service.openLink(parts[1]);
                        break;

                    case "list":
                        List<ShortLink> myLinks = service.getUserLinks(currentUserUuid);
                        if (myLinks.isEmpty()) {
                            System.out.println("У вас нет активных ссылок.");
                        } else {
                            System.out.printf("%-10s | %-10s | %-20s%n", "Code", "Visits", "Expires");
                            for (ShortLink l : myLinks) {
                                System.out.printf("%-10s | %d/%-7d | %s%n",
                                        l.getShortCode(), l.getCurrentVisits(), l.getMaxVisits(), l.getExpiresAt());
                            }
                        }
                        break;

                    case "delete":
                        if (parts.length < 2) {
                            System.out.println("Использование: delete <short_code_or_url>");
                            break;
                        }
                        service.deleteLink(parts[1], currentUserUuid);
                        break;

                    case "edit-limit":
                        if (parts.length < 3) {
                            System.out.println("Использование: edit-limit <short_code> <new_limit>");
                            break;
                        }
                        service.updateLimit(parts[1], Integer.parseInt(parts[2]), currentUserUuid);
                        break;

                    case "new-user":
                        currentUserUuid = UUID.randomUUID();
                        System.out.println("Сгенерирован новый пользователь: " + currentUserUuid);
                        break;

                    case "help":
                        printHelp();
                        break;

                    case "exit":
                        System.out.println("Завершение работы...");
                        scheduler.shutdown();
                        scanner.close();
                        System.exit(0);

                    default:
                        System.out.println("Неизвестная команда. Введите 'help'.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Лимит должен быть числом.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  shorten <url> [limit]      - Создать короткую ссылку (лимит опционален)");
        System.out.println("  open <short_url>           - Переход по ссылке (открывает браузер)");
        System.out.println("  list                       - Показать мои ссылки");
        System.out.println("  delete <short_url>         - Удалить ссылку");
        System.out.println("  edit-limit <url> <limit>   - Изменить лимит переходов");
        System.out.println("  new-user                   - Сменить пользователя (новый UUID)");
        System.out.println("  exit                       - Выход");
    }
}
