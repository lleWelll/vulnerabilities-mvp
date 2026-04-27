# Secure Code Review: точки входа пользовательских данных

## Охват аудита

Проверены все реальные точки входа пользовательских данных в MVP:

- API path/query/body: `AuthController`, `PaymentController`, `FraudController`
- "формы" в контексте REST-MVP: JSON payload для `login`, `register`, `createPayment`, `flagPayment`
- фильтры поиска: пагинация и новые фильтры истории платежей `status`, `receiverUsername`
- экспорт: новый поток `GET /api/payments/history/export`
- пути к файлам: временный экспорт в выделенный каталог
- журналирование: `AuditService`, security handlers, exception handler
- обращения к БД: JPA repository / JPQL query с параметрами

Если категории отсутствовали в исходном MVP, это зафиксировано:

- пользовательской загрузки файлов как бизнес-функции нет; `multipart/form-data` централизованно отклоняется
- внешних сервисов в бизнес-логике платежей нет

## Основной бизнес-сценарий

Сценарий: клиент просматривает и экспортирует историю своих платежей с фильтрами.

## Trust Boundaries

```text
[External Client]
    |
    |  HTTP query params + JWT + JSON
    v
[Spring Security + Filters]
    |
    |  auth, request-size guard, multipart rejection
    v
[Controller Validation Boundary]
    |
    |  allowlist / enum / @Min / @Size / @Pattern
    v
[Normalization Boundary]
    |
    |  NFKC normalization, control-char cleanup, safe filename normalization
    v
[Service Layer]
    |
    |  business rules, ownership checks
    +--------------------------+
    |                          |
    v                          v
[Parameterized JPQL / DB]   [Audit / App Logs]
    |
    v
[DTO Mapping Boundary]
    |
    +-----------------------------+
    |                             |
    v                             v
[CSV/JSON/XML Export Sink]   [Filesystem Export Directory]
    |
    v
[HTTP Response to Client]
```

## Таблица Source -> Propagation -> Sink -> Mitigation

| Источник данных | Распространение | Sink | Мера защиты |
| --- | --- | --- | --- |
| `receiverUsername`, `status`, `page`, `size` из `/api/payments/history` и `/history/export` | `PaymentController` -> `PaymentHistoryQuery` -> `PaymentService` -> `PaymentRepository` | SQL / JPQL | enum allowlist, `@Pattern`, `@Size`, нормализация `NFKC`, параметризованный `@Query` с named params |
| `fileName`, `format` из `/api/payments/history/export` | `PaymentController` -> `PaymentExportService` | файловая система | allowlist для имени, `Path.normalize()`, `resolve()`, проверка `startsWith(exportDir)` |
| `description` платежа | `CreatePaymentRequest` -> `PaymentService` -> DTO -> экспорт CSV/XML/JSON | сериализация и экспорт | очистка control chars, DTO-only export, CSV formula escaping, XML escaping |
| `username` в `login/register` | controller -> `AuthService` -> `UserRepository` + `AuditService` | DB lookup, logs | allowlist pattern, Unicode normalization, логирование только sanitized values |
| `reason` при manual fraud flag | `FraudController` -> `FraudService` -> `FraudFlagRepository` | DB + logs via audit trail | free-text normalization, length bound, DTO/API validation |

## Выявленные опасные sink и устранение

### 1. SQL sink

Риск:

- пользовательские фильтры истории платежей могли бы стать точкой для небезопасной динамической выборки.

Исправление:

- добавлен параметризованный JPQL в `PaymentRepository.findHistoryByOwnerAndFilters(...)`;
- фильтры проходят allowlist-валидацию и нормализацию до запроса.

Подтверждение в коде:

- `src/main/java/org/aitu/vulnerabilitiesmvp/repository/PaymentRepository.java`
- `src/main/java/org/aitu/vulnerabilitiesmvp/service/PaymentService.java`

### 2. Logging sink

Риск:

- пользовательские значения попадали в audit log в несанаitized виде, что открывало log injection / log forging.

Исправление:

- `AuditService` теперь логирует только уже sanitized значения, которые одновременно сохраняются в БД.

Подтверждение в коде:

- `src/main/java/org/aitu/vulnerabilitiesmvp/service/AuditService.java`

### 3. Filesystem sink

Риск:

- экспорт с пользовательским именем файла может привести к path traversal и записи вне разрешённого каталога.

Исправление:

- имя файла ограничено allowlist;
- путь нормализуется;
- выполняется проверка, что конечный `Path` остаётся внутри `app.exports.base-directory`.

Подтверждение в коде:

- `src/main/java/org/aitu/vulnerabilitiesmvp/service/PaymentExportService.java`
- `src/main/java/org/aitu/vulnerabilitiesmvp/config/AppProperties.java`

### 4. Export / serialization sink

Риск:

- CSV formula injection;
- XML/JSON export без явного контроля над сериализуемой моделью;
- возможная утечка ORM entity вместо безопасного DTO.

Исправление:

- экспорт строится только из `PaymentResponse`;
- CSV экранирует формульные префиксы `=`, `+`, `-`, `@`;
- XML экранирует спецсимволы;
- JSON пишется через `ObjectMapper` из DTO, а не из entity.

Подтверждение в коде:

- `src/main/java/org/aitu/vulnerabilitiesmvp/service/PaymentExportService.java`
- `src/main/java/org/aitu/vulnerabilitiesmvp/mapper/PaymentMapper.java`

## Подтверждение устранения опасных точек

Изменения внесены в код:

- `PaymentController`: безопасные query params и endpoint экспорта
- `PaymentService`: нормализация и защищённый поиск истории
- `PaymentRepository`: параметризованный фильтр
- `PaymentExportService`: безопасный export path + корректный CSV/JSON/XML export
- `AuditService`: устранение log injection
- `AuthService`, `FraudService`, `LoginRequest`: ужесточение нормализации и allowlist-валидации

Проверка тестами:

- экспорт CSV экранирует formula injection
- экспорт XML экранирует markup
- невалидное имя файла для экспорта отвергается
- существующие auth/payment/fraud интеграционные тесты остаются зелёными
