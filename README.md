# Vulnerabilities MVP

## Docker Compose

Локальный запуск всех зависимостей:

```bash
docker compose up --build
```

Сервисы:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- PostgreSQL: `localhost:5432`

Compose поднимает:

- `postgres:17-alpine`
- Spring Boot backend
- React frontend, собранный в статические файлы и отданный через nginx

Dev seed users создаются backend-профилем `dev`:

| Username | Password | Role |
| --- | --- | --- |
| `test_client_1` | `ClientOnePass123!` | `CLIENT` |
| `test_client_2` | `ClientTwoPass123!` | `CLIENT` |
| `test_operator` | `OperatorPass123!` | `OPERATOR` |

Остановка:

```bash
docker compose down
```

Остановка с удалением данных PostgreSQL:

```bash
docker compose down -v
```

Значения `DB_PASSWORD`, `JWT_SECRET` и seed-пароли в `docker-compose.yml` предназначены для локальной разработки. Для production передавайте их через секреты окружения/CI/CD и используйте отдельный профиль.
