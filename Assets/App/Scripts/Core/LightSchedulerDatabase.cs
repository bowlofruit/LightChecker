using UnityEngine;
using System.Collections.Generic;
using System.Linq;

[CreateAssetMenu(fileName = "LightScheduleDB", menuName = "LightChecker/Database", order = 1)]
public class LightScheduleDatabase : ScriptableObject
{
    [SerializeField]
    private List<CityConfig> cities = new List<CityConfig>();

    public CityConfig GetConfig(City city)
    {
        var config = cities.FirstOrDefault(c => c.city == city);
        if (config == null)
        {
            Debug.LogError($"[LightScheduleDatabase] Config for {city} not found!");
            return null;
        }
        return config;
    }
}