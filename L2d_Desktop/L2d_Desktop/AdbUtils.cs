using ICSharpCode.SharpZipLib.Zip;
using SharpAdbClient;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace L2d_Desktop
{
    internal class AdbUtils
    {
        private static AdbClient devices;
        private static IShellOutputReceiver receiver = new MyShellOutputReceiver();
        public static void Start()
        {
            if (!File.Exists(@"./platform-tools/adb.exe"))
            {
                Install();
            }
            AdbServer server = new();
            server.StartServer(@"./platform-tools/adb.exe", restartServerIfNewer: false);

            devices = new AdbClient();
        }

        public static List<DeviceData> GetDevices()
        {
            return devices.GetDevices();
        }

        public static Task Connect(DeviceData device)
        {
            return Task.Run(() =>
            {
                try
                {
                    devices.CreateForward(device, $"tcp:{MainWindow.Port}", "tcp:23456", true);
                    Thread.Sleep(500);
                }
                catch (Exception e)
                {
                    App.ThisApp.AddLog(e.ToString());
                }
            });
        }

        public static void Install()
        {
            using ZipInputStream s = new(new MemoryStream(Resource1.platform_tools_r33_0_1_windows));

            ZipEntry theEntry;
            while ((theEntry = s.GetNextEntry()) != null)
            {
                string directoryName = Path.GetDirectoryName(theEntry.Name);
                string fileName = Path.GetFileName(theEntry.Name);

                // create directory
                if (directoryName.Length > 0)
                {
                    Directory.CreateDirectory(directoryName);
                }

                if (fileName != string.Empty)
                {
                    using FileStream streamWriter = File.Create(theEntry.Name);
                    int size = 2048;
                    byte[] data = new byte[2048];
                    while (true)
                    {
                        size = s.Read(data, 0, data.Length);
                        if (size > 0)
                        {
                            streamWriter.Write(data, 0, size);
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            }
        }
    }

    class MyShellOutputReceiver : IShellOutputReceiver
    {
        public bool ParsesErrors => true;

        public void AddOutput(string line)
        {
            App.ThisApp.AddLog(line);
        }

        public void Flush()
        {
            
        }
    }
}
