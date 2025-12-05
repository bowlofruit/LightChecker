public class AndroidWidgetService : IWidgetService
{
    private readonly NativeBridge _bridge;

    public AndroidWidgetService(NativeBridge bridge)
    {
        _bridge = bridge;
    }

    public void UpdateWidget(string url, string regex, int interval)
    {
        _bridge.ConfigureWidget(url, regex, interval);
    }
}