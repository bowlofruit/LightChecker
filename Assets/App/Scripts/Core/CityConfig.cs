using UnityEngine;
using System;
using System.Collections.Generic;
using System.Text.RegularExpressions;

[Serializable]
public class CityConfig
{
    [Header("Basic Info")]
    public City city;
    public string providerName;

    [Header("Network")]
    [Tooltip("Посилання на WEB-версію (обов'язково https://t.me/s/...)")]
    public string telegramUrl;

    [Header("Parsing - Unity UI")]
    [Tooltip("Regex для пошуку ВСІХ черг на сторінці. Приклад: (?m)^(\\d\\.\\d)")]
    public string queueExtractionRegex;

    [Header("Parsing - Android Native")]
    [Tooltip("Шаблон Regex для Android. {0} буде замінено на вибрану чергу. Приклад: ^{0}\\s+(.*)")]
    public string scheduleRegexTemplate;

    [Header("Geo Location")]
    public Vector2 coordinates;

    /// <summary>
    /// Генерує фінальний Regex для Android (виправляє помилку в AppController)
    /// </summary>
    public string GetFinalRegex(string userQueue)
    {
        string safeQueue = Regex.Escape(userQueue);

        return string.Format(scheduleRegexTemplate, safeQueue);
    }
}