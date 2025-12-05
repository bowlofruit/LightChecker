using UnityEngine;

public class NativeBridge : MonoBehaviour
{
    private const string PLUGIN_CLASS = "com.bowlof.lightchecker.plugin.UnityBridge";

    public void ConfigureWidget(string url, string regex, int intervalMinutes)
    {
        if (Application.platform != RuntimePlatform.Android) return;

        try
        {
            using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
            using (AndroidJavaClass pluginClass = new AndroidJavaClass(PLUGIN_CLASS))
            {
                pluginClass.CallStatic(
                    "configureScheduler",
                    currentActivity,
                    url,
                    regex,
                    (long)intervalMinutes
                );
            }
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[LightChecker] JNI Error: {e.Message}");
        }
    }

    public void RequestPinWidget()
    {
        if (Application.platform != RuntimePlatform.Android) return;
        try
        {
            using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using var currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using var pluginClass = new AndroidJavaClass(PLUGIN_CLASS);

            pluginClass.CallStatic("requestPinWidget", currentActivity);
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[NativeBridge] Pin Error: {e.Message}");
        }
    }
}