public class AndroidWidgetService : IWidgetService
{
    public void UpdateWidget(string url, string regex, int interval)
    {
        NativeBridge.Instance.ConfigureWidget(url, regex, interval);
    }
}