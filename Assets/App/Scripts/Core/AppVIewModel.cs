using Cysharp.Threading.Tasks;
using R3;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class AppViewModel
{
    // Inputs (Команди від UI)
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
    private readonly ReactiveProperty<string> _statusMessage = new("");

    public AppViewModel(
        LightScheduleDatabase database,
        IWidgetService widgetService,
        TelegramParserService parserService)
    {
        _database = database;
        _widgetService = widgetService;
        _parserService = parserService;

        var cities = System.Enum.GetNames(typeof(City)).ToList();
        CityNames = Observable.Return(cities).ToReadOnlyReactiveProperty();
        StatusMessage = _statusMessage.ToReadOnlyReactiveProperty();

        SelectedCityIndex.Subscribe(async index => await LoadQueuesForCity(index));

        SaveCommand.Subscribe(_ => ApplySettings());
    }

    private async UniTask LoadQueuesForCity(int index)
    {
        var city = (City)index;
        var config = _database.GetConfig(city);
        if (config == null) return;

        IsLoading.Value = true;
        _statusMessage.Value = $"Завантаження списку черг для {city}...";
        AvailableQueues.Value = new List<string>(); // Очистити старе

        // Викликаємо парсер
        var queues = await _parserService.GetAvailableQueues(config.telegramUrl, config.queueExtractionRegex);

        IsLoading.Value = false;

        if (queues.Count > 0)
        {
            AvailableQueues.Value = queues;
            _statusMessage.Value = $"Знайдено черг: {queues.Count}. Виберіть свою.";
            SelectedQueueIndex.Value = 0;
        }
        else
        {
            _statusMessage.Value = $"Не вдалося знайти графіки на сторінці каналу.";
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

        string queue = AvailableQueues.Value[SelectedQueueIndex.Value];

        string finalRegex = string.Format(config.scheduleRegexTemplate, System.Text.RegularExpressions.Regex.Escape(queue));

        _widgetService.UpdateWidget(config.telegramUrl, finalRegex, 15);
        _statusMessage.Value = "Віджет налаштовано!";
    }
}