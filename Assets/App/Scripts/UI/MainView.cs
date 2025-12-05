using UnityEngine;
using UnityEngine.UIElements;
using Zenject;
using R3;

public class MainView : MonoBehaviour
{
    private AppViewModel _viewModel;
    private UIDocument _document;

    private DropdownField _cityDropdown;
    private DropdownField _queueDropdown;
    private Button _saveButton;
    private Button _geoButton;
    private Label _statusLabel;

    [Inject]
    public void Construct(AppViewModel viewModel)
    {
        _viewModel = viewModel;
    }

    private void OnEnable()
    {
        _document = GetComponent<UIDocument>();
        if (_document == null)
        {
            Debug.LogError("UIDocument not found!");
            return;
        }

        var root = _document.rootVisualElement;

        _cityDropdown = root.Q<DropdownField>("DropdownCity");
        _queueDropdown = root.Q<DropdownField>("DropdownQueue");
        _saveButton = root.Q<Button>("BtnSave");
        _geoButton = root.Q<Button>("BtnGeo");
        _statusLabel = root.Q<Label>("LblStatus");

        BindViewModel();
    }

    private void BindViewModel()
    {
        _viewModel.CityNames.Subscribe(cities =>
        {
            _cityDropdown.choices = cities;
            if (cities.Count > 0 && string.IsNullOrEmpty(_cityDropdown.value))
                _cityDropdown.index = 0;
        }).AddTo(this);

        // 2. Список черг
        _viewModel.AvailableQueues.Subscribe(queues =>
        {
            _queueDropdown.choices = queues;
            _queueDropdown.SetEnabled(queues.Count > 0);

            if (queues.Count > 0) _queueDropdown.index = 0;
            else _queueDropdown.value = "Завантаження...";
        }).AddTo(this);

        _viewModel.StatusMessage.Subscribe(msg => _statusLabel.text = msg).AddTo(this);

        _viewModel.SelectedCityIndex.Subscribe(idx =>
        {
            if (idx >= 0 && idx < _cityDropdown.choices.Count)
                _cityDropdown.index = idx;
        }).AddTo(this);

        _viewModel.SelectedQueueIndex.Subscribe(idx =>
        {
            if (idx >= 0 && idx < _queueDropdown.choices.Count)
                _queueDropdown.index = idx;
        }).AddTo(this);

        _viewModel.IsLoading.Subscribe(isLoading =>
        {
            _saveButton.SetEnabled(!isLoading);
            if (isLoading) _statusLabel.text = "Працюю...";
        }).AddTo(this);



        _cityDropdown.RegisterValueChangedCallback(evt =>
        {
            int index = _cityDropdown.choices.IndexOf(evt.newValue);
            if (index >= 0) _viewModel.SelectedCityIndex.Value = index;
        });

        _queueDropdown.RegisterValueChangedCallback(evt =>
        {
            int index = _queueDropdown.choices.IndexOf(evt.newValue);
            if (index >= 0) _viewModel.SelectedQueueIndex.Value = index;
        });

        _saveButton.clicked += () => _viewModel.SaveCommand.OnNext(Unit.Default);
        _geoButton.clicked += () => _viewModel.AutoDetectLocationCommand.OnNext(Unit.Default);
    }
}