using UnityEngine;
using App.Core;

namespace App.Services
{
    public class AndroidWidgetService : INativeWidgetService
    {
        private const string JAVA_CLASS_NAME = "com.Bowlof.LightChecker.SharedData";

        public void UpdateWidgetConfig(string dataUrl, string regexPattern, string widgetTitle)
        {
            if (Application.isEditor)
            {
                Debug.Log($"[Mock Android] Sending: {widgetTitle}, URL: {dataUrl}");
                return;
            }

            if (Application.platform == RuntimePlatform.Android)
            {
                try
                {
                    using var sharedDataClass = new AndroidJavaClass(JAVA_CLASS_NAME);
                    using var context = GetCurrentActivity();
                    sharedDataClass.CallStatic("saveSettings", context, dataUrl, regexPattern, widgetTitle);
                    Debug.Log("Android Bridge: Settings sent successfully!");
                }
                catch (System.Exception e)
                {
                    Debug.LogError($"Android Bridge Error: {e.Message}");
                }
            }
        }

        private AndroidJavaObject GetCurrentActivity()
        {
            using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            return unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
        }
    }
}