public class AndroidWidgetService : IWidgetService
{
    private readonly NativeBridge _bridge;

    public AndroidWidgetService(NativeBridge bridge)
    {
        _bridge = bridge;
    }

    public void UpdateWidget(string url, string scheduleRegex, string dateRegex, int interval)
    {
        _bridge.ConfigureWidget(url, scheduleRegex, dateRegex, interval);
    }
}