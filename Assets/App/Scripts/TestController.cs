using UnityEngine;
using App.Core;
using App.Services;
using App.Data;

namespace App
{
    public class TestController : MonoBehaviour
    {
        [Header("Test Data")]
        public CityConfig TestCity; // Перетягни сюди конфіг
        public string TestQueue = "1.1";

        private INativeWidgetService _widgetService;

        private void Awake()
        {
            // У простій версії створюємо сервіс вручну
            _widgetService = new AndroidWidgetService();
        }

        private void OnGUI()
        {
            // Проста кнопка на екрані телефону для тесту
            GUI.matrix = Matrix4x4.TRS(Vector3.zero, Quaternion.identity, Vector3.one * 3); // Збільшуємо GUI

            if (GUILayout.Button("Update Widget: Test City"))
            {
                SendUpdate();
            }
        }

        // Цей метод можна викликати також через ПКМ в інспекторі
        [ContextMenu("Test Update Widget")]
        public void SendUpdate()
        {
            if (TestCity == null)
            {
                Debug.LogError("City Config is missing!");
                return;
            }

            // 1. Формуємо Regex (підставляємо чергу замість {0})
            string finalRegex = string.Format(TestCity.RegexTemplate, TestQueue);

            // 2. Формуємо заголовок
            string title = $"{TestCity.CityName} - {TestQueue}";

            // 3. Відправляємо
            _widgetService.UpdateWidgetConfig(TestCity.DataUrl, finalRegex, title);

            Debug.Log($"Sent to Android:\nTitle: {title}\nRegex: {finalRegex}");
        }
    }
}