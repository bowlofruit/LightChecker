using UnityEngine;

public class NativeBridge : MonoBehaviour
{
    public static NativeBridge Instance { get; private set; }

    private const string PLUGIN_CLASS = "com.bowlof.lightchecker.plugin.UnityBridge";

    void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
        }
    }

    /// <summary>
    /// Налаштовує віджет і запускає фоновий парсинг.
    /// </summary>
    /// <param name="url">URL для парсингу (наприклад, .txt файл або легка html сторінка)</param>
    /// <param name="regex">Регулярка. Перша група (.*) буде відображена.</param>
    /// <param name="intervalMinutes">Частота оновлення (мін. 15 хв).</param>
    public void ConfigureWidget(string url, string regex, int intervalMinutes)
    {
        if (Application.platform != RuntimePlatform.Android)
        {
            Debug.Log($"[MOCK] Configuring Widget: URL={url}, Regex={regex}");
            return;
        }

        try
        {
            using AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using AndroidJavaClass pluginClass = new AndroidJavaClass(PLUGIN_CLASS);

            pluginClass.CallStatic(
                "configureScheduler",
                currentActivity,
                url,
                regex,
                (long)intervalMinutes
            );

            Debug.Log("[LightChecker] Configuration sent to Android Native successfully!");
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[LightChecker] JNI Error: {e.Message}");
        }
    }

    /// <summary>
    /// Примусово оновлює дані (для кнопки "Оновити зараз" в Unity).
    /// </summary>
    public void ForceUpdate()
    {
        if (Application.platform != RuntimePlatform.Android) return;

        try
        {
            using AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using AndroidJavaClass pluginClass = new AndroidJavaClass(PLUGIN_CLASS);
            pluginClass.CallStatic("forceUpdate", currentActivity);
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[LightChecker] Force Update Error: {e.Message}");
        }
    }
}
