# Elementary — игровое ядро (game-core)

Правила игры **Elementary** в виде чистой Java-библиотеки без Spring и без ввода-вывода (см. [ADR 0004](https://github.com/solomain-games/elementary-docs/blob/main/adr/0004-game-core-pure-java.md)).

| Пакет | Содержимое |
|---|---|
| `model` | неизменяемые данные: дело, карты, игроки, состояние партии |
| `engine` | правила: подготовка партии, обработка команд |
| `view` | представление состояния для конкретного игрока |
| `error` | ошибки правил |

## Сборка

Требуется JDK 25. Maven устанавливать не обязательно, в репозитории есть Maven Wrapper.

```powershell
.\mvnw verify      # Windows
./mvnw verify      # Linux, macOS
```

`verify` компилирует код, запускает тесты и собирает jar в `target/`.
