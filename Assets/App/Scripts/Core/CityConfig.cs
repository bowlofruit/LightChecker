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

    [Header("Date Parsing (Advanced)")]
    [Tooltip("Регулярка для пошуку дат у тексті. Має знаходити: '05.12', '5 грудня', 'завтра'.")]
    [TextArea(2, 5)]
    public string dateDetectionRegex = @"(?i)(\d{1,2}[./]\d{2})|(\d{1,2}\s+(січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня))|(завтра)|(сьогодні)";

    [Header("Geo Location")]
    public Vector2 coordinates;
}