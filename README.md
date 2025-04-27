# Archery Battle Game 🏹

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.6-orange)](https://openjfx.io)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

Многопользовательская сетевая игра в реальном времени с использованием JavaFX, разработанная в рамках курса "Разработка сетевых приложений на Java"

## 🌟 Особенности
- 🕹️ Мультиплеер до 4 игроков (проект работает на одном компьютере, где клиенты запускаются в разных окнах)
- 🎯 Динамическая система мишеней с разными размерами
- ⏸️ Пауза и возобновление игры
- 🏆 Автоматическое завершение при достижении 200 очков
- 📊 Статистика игроков в реальном времени
- 🔄 Возврат в лобби после завершения матча

_Для настройки параметров, вы можете изменить ```Config.java```, например константа ```GAME_WIN_SCORE``` отвечает за победный счёт_

### Требования
- Java 17+
- Maven 3.6+
- JavaFX SDK 17.0.6

### Установка
~~~
git clone https://github.com/petr1core/java_lan_2.git
~~~
~~~
cd java_lan_2
~~~
~~~
mvn clean install
~~~
