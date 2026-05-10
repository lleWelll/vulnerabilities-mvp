# Аудит аутентификации, авторизации и криптографии

## 1. Матрица доступа по ролям

| API / действие | `ANONYMOUS` | `CLIENT` | `OPERATOR` | Серверное правило принятия решения |
| --- | --- | --- | --- | --- |
| `POST /api/auth/register` | Да | Да | Да | Публичная регистрация, новая роль всегда `CLIENT` |
| `POST /api/auth/login` | Да | Да | Да | Проверка username/password, выдача JWT |
| `POST /api/payments` | Нет | Да | Нет | Нужна роль `CLIENT`, `sourceAccountId` должен принадлежать текущему пользователю |
| `POST /api/payments/{id}/confirm` | Нет | Да | Нет | Нужна роль `CLIENT`, подтверждать можно только собственный платёж |
| `GET /api/payments/history` | Нет | Да | Нет | Нужна роль `CLIENT`, история возвращается только по `principal.id` |
| `GET /api/payments/history/export` | Нет | Да | Нет | Нужна роль `CLIENT`, экспорт строится только из собственной истории |
| `GET /api/payments/{id}` | Нет | Да | Условно | `CLIENT` видит только свой платёж; `OPERATOR` видит только `flagged`-платёж |
| `GET /api/fraud/flags` | Нет | Нет | Да | Нужна роль `OPERATOR` |
| `POST /api/fraud/payments/{id}/flag` | Нет | Нет | Да | Нужна роль `OPERATOR` |
| `GET /swagger-ui/**`, `GET /v3/api-docs/**` | Нет | Нет | Да | API-документация доступна только оператору |

## 2. Проверка хранения паролей, токенов и секретов

| Область | Состояние | Вывод |
| --- | --- | --- |
| Хранение паролей | `BCryptPasswordEncoder(12)` и ограничение длины пароля до 72 символов | Корректно для MVP; усечению bcrypt сверх 72 символов предотвращено DTO-валидацией |
| Выдача JWT | Короткоживущий access token, срок по умолчанию 15 минут | Приемлемо для MVP |
| Хранение JWT в frontend | Access token хранится только в `sessionStorage`, legacy `localStorage` значение удаляется | Снижает риск долгоживущей кражи Bearer токена при XSS |
| Проверка JWT | После усиления валидируются `sub`, `iss`, `uid`, `role`, `exp`; при выпуске добавлены `jti` и `nbf` | Лучше защищает от подмены claims и от приема токенов вне контекста выпуска |
| Захардкоженные секреты | Production JWT secret читается из `JWT_SECRET`; dev seed-пароли читаются из `secret.env` / env | В runtime-пути production захардкоженных паролей не обнаружено |
| Тестовые пароли | В `src/test` сохранены известные учётки для интеграционных тестов | Это не production-риск, но в отчёте должно быть отмечено как допустимое исключение тестового контура |

## 3. Найденные authorization / crypto риски

| Риск | До исправления | Мера |
| --- | --- | --- |
| Слишком грубый доступ к `GET /api/payments/history` | Любой аутентифицированный пользователь проходил через `authenticated()` | История и экспорт закреплены только за `CLIENT` и дополнительно защищены на service-слое |
| Доступ оператора к любому платёжному объекту | `OPERATOR` мог читать любой `payment`, даже не связанный с fraud-review | Введено объектное правило: оператор видит только `flagged`-платежи |
| Проверка JWT не связывала token claims с текущим principal | В токене были `uid` и `role`, но они не проверялись | Добавлена сверка `iss`, `uid`, `role`, а также `jti` / `nbf` при выпуске |
| Отказ по role-based access не попадал в audit trail | 403 на уровне Spring Security только логировался в application log | `RestAccessDeniedHandler` теперь пишет `ACCESS_DENIED` в `audit_event` |
| Отсутствие revocation / refresh flow | Access token статичен до истечения срока жизни | Остаточный риск MVP; для production нужен refresh-token / denylist / rotation |
| Неограниченные login attempts | Brute-force был ограничен только стоимостью bcrypt | Добавлен lockout после серии ошибок и audit для throttled login |

## 4. Сценарии принятия решения о доступе

### Легитимный сценарий

Запрос: `OPERATOR -> GET /api/payments/2001`

1. `SecurityConfig` пропускает запрос, потому что endpoint требует только аутентификацию.
2. `PaymentService` загружает платёж.
3. `AuthorizationService.requirePaymentReadAccess(...)` видит, что пользователь `OPERATOR`, а платёж `flagged=true`.
4. Доступ разрешается, ответ `200 OK`.

### Запрещённый сценарий

Запрос: `OPERATOR -> GET /api/payments/2002`

1. `SecurityConfig` пропускает запрос как аутентифицированный.
2. `PaymentService` загружает платёж.
3. `AuthorizationService.requirePaymentReadAccess(...)` видит, что пользователь не владелец и платёж не `flagged`.
4. В `audit_event` пишется `ACCESS_DENIED`.
5. Возвращается `403 Forbidden`.

## 5. Архитектурное обоснование

- Решение о доступе к конкретному объекту вынесено в отдельный `AuthorizationService`, чтобы не размазывать policy по контроллерам и бизнес-логике.
- Role-based защита оставлена в `SecurityConfig` как внешний барьер, а object-level проверка добавлена в сервисы как второй обязательный слой.
- JWT усилен минимально инвазивно: без смены клиентского контракта, но с обязательной проверкой контекста выпуска токена.
- Audit trail расширен на security-layer запреты, потому что чувствительные попытки доступа должны попадать не только в application log, но и в постоянное хранилище событий.
