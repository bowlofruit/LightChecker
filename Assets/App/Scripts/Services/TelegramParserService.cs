using Cysharp.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using R3;

public class TelegramParserService
{
    private readonly NativeBridge _nativeBridge;

    public TelegramParserService(NativeBridge nativeBridge)
    {
        _nativeBridge = nativeBridge;
    }

    public async UniTask<List<string>> GetAvailableQueues(string url, string regexPattern)
    {
        var tcs = new UniTaskCompletionSource<List<string>>();

        using var sub1 = _nativeBridge.OnQueuesReceived.Take(1).Subscribe(queues =>
        {
            tcs.TrySetResult(queues.ToList());
        });

        using var sub2 = _nativeBridge.OnQueuesError.Take(1).Subscribe(error =>
        {
            Debug.LogError($"[Parser] OCR Error: {error}");
            tcs.TrySetResult(new List<string>());
        });

        _nativeBridge.FetchQueues(url, regexPattern);

        var (isCanceled, result) = await tcs.Task
            .Timeout(System.TimeSpan.FromSeconds(15))
            .SuppressCancellationThrow();

        if (isCanceled)
        {
            Debug.LogError("[Parser] Operation timed out (Android did not respond in 15s).");
            return new List<string>();
        }

        return result;
    }
}