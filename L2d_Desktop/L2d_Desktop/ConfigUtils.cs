using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace L2d_Desktop;

public class ConfigUtils
{
    /// <summary>
    /// 读取JSON
    /// </summary>
    /// <typeparam name="T">类型</typeparam>
    /// <param name="obj">初始化对象</param>
    /// <param name="file"></param>
    /// <returns></returns>
    public static T? Config<T>(string file, T? obj) where T : new()
    {
        if (!File.Exists(file))
        {
            if (obj == null)
                obj = new T();

            Save(obj, file);
        }
        else
        {
            obj = JsonConvert.DeserializeObject<T>(File.ReadAllText(file));
        }
        return obj;
    }

    /// <summary>
    /// 保存为JSON
    /// </summary>
    /// <param name="obj">对象</param>
    /// <param name="file">文件名</param>
    public static void Save(object obj, string file)
    {
        string temp = JsonConvert.SerializeObject(obj, Formatting.Indented);
        byte[] temp1 = Encoding.UTF8.GetBytes(temp);
        using var stream = File.Open(file, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
        stream.Write(temp1);
    }

    /// <summary>
    /// 保存为JSON
    /// </summary>
    /// <param name="obj">对象</param>
    /// <param name="file">文件名</param>
    public static ValueTask SaveAsync(object obj, string file)
    {
        string temp = JsonConvert.SerializeObject(obj, Formatting.Indented);
        byte[] temp1 = Encoding.UTF8.GetBytes(temp);
        using var stream = File.Open(file, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
        return stream.WriteAsync(temp1);
    }
}
