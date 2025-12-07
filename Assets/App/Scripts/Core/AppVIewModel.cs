using Cysharp.Threading.Tasks;
using R3;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class AppViewModel
{
    public ReactiveProperty<int> SelectedCityIndex { get; } = new(0);
    public ReactiveProperty<int> SelectedQueueIndex { get; } = new(0);
    public Subject<Unit> SaveCommand { get; } = new();
    public Subject<Unit> AutoDetectLocationCommand { get; } = new();

    public ReadOnlyReactiveProperty<List<string>> CityNames { get; }
    public ReactiveProperty<List<string>> AvailableQueues { get; } = new(new List<string>());
    public ReactiveProperty<bool> IsLoading { get; } = new(false);
    public ReadOnlyReactiveProperty<string> StatusMessage { get; }

    private readonly LightScheduleDatabase _database;
    private readonly IWidgetService _widgetService;
    private readonly TelegramParserService _parserService;
    private readonly CityLocationService _locationService;
    private readonly NativeBridge _nativeBridge;

    private readonly ReactiveProperty<string> _statusMessage = new("");

    public AppViewModel(
        LightScheduleDatabase database,
        IWidgetService widgetService,
        TelegramParserService parserService,
        CityLocationService locationService,
        NativeBridge nativeBridge)
    {
        _database = database;
        _widgetService = widgetService;
        _parserService = parserService;
        _locationService = locationService;
        _nativeBridge = nativeBridge;

        var cities = System.Enum.GetNames(typeof(City)).ToList();
        CityNames = Observable.Return(cities).ToReadOnlyReactiveProperty();
        StatusMessage = _statusMessage.ToReadOnlyReactiveProperty();

        SelectedCityIndex.Subscribe(async index => await LoadQueuesForCity(index));

        SaveCommand.Subscribe(_ => ApplySettings());

        AutoDetectLocationCommand.Subscribe(_ => DetectLocation());
    }

    private async void DetectLocation()
    {
        IsLoading.Value = true;
        _statusMessage.Value = "Отримання GPS даних...";

        var detectedCity = await _locationService.DetectCity(_database);

        IsLoading.Value = false;
        if (detectedCity.HasValue)
        {
            SelectedCityIndex.Value = (int)detectedCity.Value;
            _statusMessage.Value = $"Локація визначена: {detectedCity.Value}";
        }
        else
        {
            _statusMessage.Value = "Не вдалося визначити місто (перевірте GPS).";
        }
    }

    private async UniTask LoadQueuesForCity(int index)
    {
        var city = (City)index;
        var config = _database.GetConfig(city);
        if (config == null) return;

        IsLoading.Value = true;
        _statusMessage.Value = $"Завантаження даних для {city}...";
        AvailableQueues.Value = new List<string>();

        var queues = await _parserService.GetAvailableQueues(config.telegramUrl, config.queueExtractionRegex);

        IsLoading.Value = false;

        if (queues.Count > 0)
        {
            AvailableQueues.Value = queues;
            _statusMessage.Value = $"Знайдено черг: {queues.Count}.";
            SelectedQueueIndex.Value = 0;
        }
        else
        {
            _statusMessage.Value = $"Не вдалося знайти графіки на сторінці.";
        }
    }

    private void ApplySettings()
    {
        if (AvailableQueues.Value.Count == 0)
        {
            _statusMessage.Value = "Спочатку дочекайтеся завантаження черг!";
            return;
        }

        var city = (City)SelectedCityIndex.Value;
        var config = _database.GetConfig(city);

        if (SelectedQueueIndex.Value >= AvailableQueues.Value.Count)
            SelectedQueueIndex.Value = 0;

        string queue = AvailableQueues.Value[SelectedQueueIndex.Value];
        string finalRegex = string.Format(config.scheduleRegexTemplate, System.Text.RegularExpressions.Regex.Escape(queue));

        _widgetService.UpdateWidget(config.telegramUrl, finalRegex, config.dateDetectionRegex, 15);

        if (!PlayerPrefs.HasKey("WidgetPinnedPrompt"))
        {
            _nativeBridge.RequestPinWidget();
            PlayerPrefs.SetInt("WidgetPinnedPrompt", 1);
        }
        _statusMessage.Value = "Віджет налаштовано успішно!";
    }
}