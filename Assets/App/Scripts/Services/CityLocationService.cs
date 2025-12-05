using Cysharp.Threading.Tasks;
using UnityEngine;
using UnityEngine.Android;


public class CityLocationService
{
    public async UniTask<City?> DetectCity(LightScheduleDatabase database)
    {
        if (!Permission.HasUserAuthorizedPermission(Permission.CoarseLocation))
        {
            bool permissionResult = await RequestPermissionAsync(Permission.CoarseLocation);
            if (!permissionResult)
            {
                Debug.LogError("[CityLocationService] User denied permission");
                return null;
            }
        }

        if (!Input.location.isEnabledByUser)
        {
            Debug.LogWarning("[CityLocationService] Location disabled by user settings");
            return null;
        }

        // 3. Запускаємо сервіс
        Input.location.Start(500, 500);

        int maxWait = 20;
        while (Input.location.status == LocationServiceStatus.Initializing && maxWait > 0)
        {
            await UniTask.Delay(1000);
            maxWait--;
        }

        if (Input.location.status == LocationServiceStatus.Failed || maxWait <= 0)
        {
            Input.location.Stop();
            return null;
        }

        float lat = Input.location.lastData.latitude;
        float lon = Input.location.lastData.longitude;
        Input.location.Stop();

        Debug.Log($"[CityLocationService] Location: {lat}, {lon}");

        City? nearestCity = null;
        float minDistance = 100f;

        foreach (var cityEnum in System.Enum.GetValues(typeof(City)))
        {
            var config = database.GetConfig((City)cityEnum);
            if (config == null) continue;

            float dist = Vector2.Distance(new Vector2(lat, lon), config.coordinates);

            if (dist < minDistance)
            {
                minDistance = dist;
                nearestCity = config.city;
            }
        }

        return nearestCity;
    }

    private UniTask<bool> RequestPermissionAsync(string permission)
    {
        var tcs = new UniTaskCompletionSource<bool>();
        var callbacks = new PermissionCallbacks();

        callbacks.PermissionGranted += (p) => { tcs.TrySetResult(true); };

        callbacks.PermissionDenied += (p) => { tcs.TrySetResult(false); };

        Permission.RequestUserPermission(permission, callbacks);

        return tcs.Task;
    }
}