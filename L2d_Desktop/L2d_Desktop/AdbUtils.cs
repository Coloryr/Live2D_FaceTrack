using SharpAdbClient;
using System;
using System.Collections.Generic;
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
            AdbServer server = new AdbServer();
            server.StartServer(@"adb.exe", restartServerIfNewer: false);

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
                    //devices.ExecuteRemoteCommand("am broadcast -a Live2dStop", device, receiver);
                    //Thread.Sleep(500);
                    devices.CreateForward(device, $"tcp:{MainWindow.Port}", "tcp:23456", true);
                    Thread.Sleep(500);
                    devices.ExecuteRemoteCommand("am broadcast -a Live2dStart", device, receiver);
                    Thread.Sleep(500);
                }
                catch (Exception e)
                {
                    App.ThisApp.AddLog(e.ToString());
                }
            });
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
