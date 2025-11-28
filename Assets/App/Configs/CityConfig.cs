using UnityEngine;

namespace App.Data
{
    [CreateAssetMenu(fileName = "NewCity", menuName = "App/City Config")]
    public class CityConfig : ScriptableObject
    {
        public string CityName;

        [Header("Parsing Settings")]
        [Tooltip("URL ресурсу. Можна використовувати {0} для підстановки токена, якщо треба.")]
        public string DataUrl;

        [Tooltip("Regex патерн. Використовуй {0} там, де має бути номер черги.")]
        [TextArea(3, 5)]
        public string RegexTemplate;

        [Header("Queues")]
        public string[] AvailableQueues;
    }
}