using Zenject;
using UnityEngine;

public class AppInstaller : MonoInstaller
{
    [SerializeField] private LightScheduleDatabase database;

    public override void InstallBindings()
    {
        Container.BindInstance(database).AsSingle();

        Container.Bind<NativeBridge>()
            .FromNewComponentOnNewGameObject()
            .WithGameObjectName("NativeBridge_Service")
            .AsSingle();

        Container.Bind<IWidgetService>().To<AndroidWidgetService>().AsSingle();

        Container.Bind<CityLocationService>().AsSingle();
        Container.Bind<TelegramParserService>().AsSingle();
        Container.Bind<AppViewModel>().AsSingle();
    }
}