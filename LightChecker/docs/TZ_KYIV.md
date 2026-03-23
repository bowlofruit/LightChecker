# Europe/Kyiv — єдине джерело календарного дня

- **Firestore поле `d`:** `YYYYMMDD` у календарі **Europe/Kyiv** (не `LocalDate.now()` пристрою без TZ).
- **Kotlin:** `com.bowlof.lightchecker.domain.time.KyivTime` → `ZoneId.of("Europe/Kyiv")`.
- **Cloud Functions (Node):** використовуйте `Intl` / `luxon` / `date-fns-tz` з IANA `Europe/Kyiv` для обчислення «сьогодні» та «завтра» перед записом `d` (див. `functions/src/kyivDate.ts`).

Усі шари (Functions, Firestore, Android) повинні узгоджувати семантику `d` з цим TZ.
