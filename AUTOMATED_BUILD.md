# Автоматическая сборка и публикация APK без Android Studio

Проект `C8Calculations` собирается, тестируется, подписывается и публикуется через GitHub Actions. Основной workflow: `.github/workflows/android-build.yml`.

## Когда запускается workflow
- Pull request в `main`: проверка unit-тестов и debug-сборки.
- Push/merge в `main`: проверка тестов, затем signed release-сборка и публикация.
- `workflow_dispatch`: ручной запуск при необходимости.

## Проверка pull request
Job `test-debug`:
1. Checkout исходников.
2. JDK 17.
3. Настройка Gradle-кэша через `gradle/actions/setup-gradle@v6` with `cache-provider: basic`.
4. Unit-тесты через Gradle Wrapper: `./gradlew --no-daemon :app:testDebugUnitTest`.
5. Debug-сборка через Gradle Wrapper: `./gradlew --no-daemon :app:assembleDebug`.

Версия Gradle зафиксирована в репозитории через Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`) и сейчас равна 9.6.0. Локальная сборка и GitHub Actions используют один и тот же Wrapper.

Release-job на pull request не запускается. Секреты подписи не нужны для обычной PR-проверки.

## Release после попадания в main
После успешного `test-debug` job `release`:
1. Читает `versionName` из `app/build.gradle.kts`.
2. Восстанавливает постоянный C8 signing key из защищённых GitHub Actions secrets.
3. Проверяет keystore и alias.
4. Выполняет `:app:assembleRelease`.
5. Проверяет подпись и сертификат APK через `apksigner`.
6. Переименовывает файл в `C8Calculations-v<versionName>-release.apk`.
7. Создаёт постоянный GitHub Release `v<versionName>`, если такого Release ещё нет.
8. Прикладывает подписанный APK к GitHub Release.
9. После успешной постоянной публикации удаляет старые Android Actions artifacts из ветки `main`.
10. Пытается сохранить дополнительный временный Actions artifact на 3 дня.

GitHub Release является основным и постоянным местом скачивания APK. Actions artifact — только временная копия и его отсутствие не делает опубликованный Release недействительным.

## Где скачивать APK
Открыть репозиторий → **Releases** → нужная версия → **Assets** → `C8Calculations-v<version>-release.apk`.

Для версии 1.1.11:
- Tag/Release: `v1.1.11`
- APK: `C8Calculations-v1.1.11-release.apk`

Репозиторий публичный, поэтому Release и его APK доступны без авторизации в GitHub.

## Версионирование
Перед выпуском новой версии необходимо:
- увеличить `versionCode`;
- изменить `versionName`.

Workflow автоматически использует новое `versionName` в имени APK и теге Release. Хардкодить номер версии в workflow не требуется.

## Постоянная подпись и обновление поверх старой версии
Release APK всегда подписывается одним постоянным ключом. Это необходимо, чтобы Android позволял устанавливать новую версию поверх ранее установленной без удаления приложения.

Секреты GitHub Actions:
- `C8_KEYSTORE_BASE64`
- `C8_KEYSTORE_PASSWORD`
- `C8_KEY_ALIAS`
- `C8_KEY_PASSWORD`

Keystore и пароли не должны попадать в git.

## Хранение файлов
- GitHub Releases: постоянное хранение стабильных APK.
- GitHub Actions artifact: временно, `retention-days: 3`.
- Старые Android Actions artifacts ветки `main` очищаются автоматически после успешной публикации Release.
- Исходный код и commits при очистке artifacts не удаляются.

6 октября 2026 старая схема хранения APK в Actions на 30 дней исчерпала artifact quota. Исторические Android artifacts были удалены, а постоянное хранение перенесено в GitHub Releases. GitHub может пересчитывать освобождённую artifact quota 6-12 часов, поэтому сразу после очистки временный artifact может не загрузиться; постоянный GitHub Release при этом остаётся доступным.

## Текущая проверенная версия
- Version: 1.1.11 / versionCode 113.
- Unit tests: SUCCESS.
- Debug build: SUCCESS.
- Signed release build: SUCCESS.
- APK signature verification: SUCCESS.
- GitHub Release `v1.1.11`: published successfully.
