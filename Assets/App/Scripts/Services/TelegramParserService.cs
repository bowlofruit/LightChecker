using Cysharp.Threading.Tasks; // UniTask (або використовуй Coroutines/Task)
using UnityEngine;
using UnityEngine.Networking;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using System.Linq;

public class TelegramParserService
{
    public async UniTask<List<string>> GetAvailableQueues(string url, string regexPattern)
    {
        using var request = UnityWebRequest.Get(url);
        await request.SendWebRequest();

        if (request.result != UnityWebRequest.Result.Success)
        {
            Debug.LogError($"[TelegramParser] Network Error: {request.error}");
            return new List<string>();
        }

        string rawHtml = request.downloadHandler.text;

        string cleanText = Regex.Replace(rawHtml, @"<br\s*/?>", "\n", RegexOptions.IgnoreCase);
        cleanText = Regex.Replace(cleanText, "<.*?>", string.Empty);
        cleanText = System.Net.WebUtility.HtmlDecode(cleanText);

        var foundQueues = new HashSet<string>();
        
        var regex = new Regex(regexPattern);
        var matches = regex.Matches(cleanText);

        foreach (Match match in matches)
        {
            if (match.Success && match.Groups.Count > 1)
            {
                string queueName = match.Groups[1].Value.Trim();
                foundQueues.Add(queueName);
            }
        }

        return foundQueues.OrderBy(q => q).ToList();
    }
}