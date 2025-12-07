using System;
using R3;
using UnityEngine;

public class NativeBridge : MonoBehaviour
{
    private const string PLUGIN_CLASS = "com.bowlof.lightchecker.plugin.UnityBridge";
    public Subject<string[]> OnQueuesReceived { get; } = new();
    public Subject<string> OnQueuesError { get; } = new();

    public void ConfigureWidget(string url, string scheduleRegex, string dateRegex, int intervalMinutes)
    {
        if (Application.platform != RuntimePlatform.Android) return;

        try
        {
            using AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using AndroidJavaClass pluginClass = new AndroidJavaClass(PLUGIN_CLASS);

            pluginClass.CallStatic("configureScheduler", currentActivity, url, scheduleRegex, dateRegex, (long)intervalMinutes);
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[LightChecker] JNI Error: {e.Message}");
        }
    }

    public void FetchQueues(string url, string regex)
    {
        if (Application.platform != RuntimePlatform.Android)
        {
            Debug.Log("[NativeBridge] Mocking Android OCR call...");
            Observable.Timer(TimeSpan.FromSeconds(1))
                .Subscribe(_ => OnQueuesReceived.OnNext(new[] { "1.1", "1.2", "TEST" }));
            return;
        }

        try
        {
            using var pluginClass = new AndroidJavaClass(PLUGIN_CLASS);
            pluginClass.CallStatic("fetchQueuesFromImage", url, regex, this.name, "HandleAndroidMessage");
        }
        catch (Exception e)
        {
            Debug.LogError($"[NativeBridge] Error: {e.Message}");
            OnQueuesError.OnNext(e.Message);
        }
    }

    public void HandleAndroidMessage(string message)
    {
        Debug.Log($"[NativeBridge] Received from Android: {message}");

        if (message.StartsWith("ERROR:"))
        {
            OnQueuesError.OnNext(message.Replace("ERROR:", ""));
        }
        else
        {
            string[] queues = message.Split(',');
            OnQueuesReceived.OnNext(queues);
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

    public void SetNotifications(bool enabled)
    {
        using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        using var currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
        using var pluginClass = new AndroidJavaClass(PLUGIN_CLASS);

        pluginClass.CallStatic("setNotificationsEnabled", currentActivity, enabled);
    }
}