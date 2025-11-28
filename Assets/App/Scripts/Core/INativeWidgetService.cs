namespace App.Core
{
    public interface INativeWidgetService
    {
        void UpdateWidgetConfig(string dataUrl, string regexPattern, string widgetTitle);
    }
}