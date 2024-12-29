# URL Shortener Service

Сервис для сокращения ссылок на Java 17 + Spring Boot.

## Что умеет

- Создавать короткие ссылки.
- Делать редирект по короткому коду.
- Показывать информацию и статистику переходов.
- Кэшировать ссылки в Redis.
- Работать с Telegram-ботом.

## Технологии

- Java 17
- Spring Boot 3
- PostgreSQL
- Redis
- Flyway
- Maven

## Быстрый запуск

1. Поднимите инфраструктуру:

```bash
docker-compose up -d
```

2. Запустите приложение:

```bash
mvn spring-boot:run
```

Сервис будет доступен на `http://localhost:8080`.

Полезные ссылки:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/api-docs`
- Health: `http://localhost:8080/actuator/health`

## Основные переменные окружения

- `DB_HOST` (по умолчанию `localhost`)
- `DB_PORT` (по умолчанию `5432`)
- `DB_NAME` (по умолчанию `url_shortener`)
- `DB_USER` (по умолчанию `postgres`)
- `DB_PASSWORD` (по умолчанию `postgres`)
- `REDIS_HOST` (по умолчанию `localhost`)
- `REDIS_PORT` (по умолчанию `6379`)
- `APP_BASE_URL` (по умолчанию `http://localhost:8080`)
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`

## API

Создать короткую ссылку:

```http
POST /api/v1/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com"
}
```

Редирект:

```http
GET /{shortCode}
```

Другие методы:

- `GET /api/v1/urls`
- `GET /api/v1/urls/{code}`
- `GET /api/v1/urls/{code}/stats`
- `PUT /api/v1/urls/{code}`
- `DELETE /api/v1/urls/{code}`

## Telegram-бот

Команды:

- `/shorten https://example.com`
- `/info aB3xY9z`
- `/stats aB3xY9z`
- `/help`

Также можно просто отправить URL в чат.

## Тесты

```bash
mvn test
```

Интеграционные тесты:

```bash
mvn verify
```
