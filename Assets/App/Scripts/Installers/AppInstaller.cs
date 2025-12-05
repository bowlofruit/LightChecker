using Zenject;
using UnityEngine;

public class AppInstaller : MonoInstaller
{
    [SerializeField] private LightScheduleDatabase database;

    public override void InstallBindings()
    {
        // 1. Біндимо базу даних
        Container.BindInstance(database).AsSingle();

        // 2. Біндимо сервіси
        Container.Bind<IWidgetService>().To<AndroidWidgetService>().AsSingle();
        Container.Bind<CityLocationService>().AsSingle();

        // 3. Біндимо ViewModel
        Container.Bind<AppViewModel>().AsSingle();
    }
}