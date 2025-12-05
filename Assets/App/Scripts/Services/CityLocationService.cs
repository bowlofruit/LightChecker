using Cysharp.Threading.Tasks;
using UnityEngine;
using R3;
using UnityEngine.Android;

public class CityLocationService
{
    public async UniTask<City?> DetectCity(LightScheduleDatabase database)
    {
        if (!Permission.HasUserAuthorizedPermission(Permission.CoarseLocation))
        {
            Permission.RequestUserPermission(Permission.CoarseLocation);

            await UniTask.Delay(1000);

            if (!Permission.HasUserAuthorizedPermission(Permission.CoarseLocation))
            {
                Debug.LogError("User denied location permission");
                return null;
            }
        }

        if (!Input.location.isEnabledByUser) return null;
        Input.location.Start(500, 500);

        Input.location.Start();

        int maxWait = 20;
        while (Input.location.status == LocationServiceStatus.Initializing && maxWait > 0)
        {
            await UniTask.Delay(1000);
            maxWait--;
        }

        if (Input.location.status == LocationServiceStatus.Failed) return null;

        float lat = Input.location.lastData.latitude;
        float lon = Input.location.lastData.longitude;
        Input.location.Stop();

        City? nearestCity = null;
        float minDistance = 50f;

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
}