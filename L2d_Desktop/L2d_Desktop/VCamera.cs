using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Runtime.InteropServices.ComTypes;
using DirectShow;
using System.Runtime.InteropServices;
using System.IO.MemoryMappedFiles;
using System.IO;
using System.Drawing.Imaging;
using System.Net.Sockets;
using System.Net;
using System.Threading;

namespace L2d_Desktop
{
    internal class VCamera
    {
        private static Cmd cmd = new();
        private static MemoryMappedFile mmf;
        private static MemoryMappedFile mmf1;
        private static Socket server;
        public static void Test()
        {
            mmf = MemoryMappedFile.CreateNew("Live2dFaceTrackConfig", 30, MemoryMappedFileAccess.ReadWrite);
            server = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            server.Bind(new IPEndPoint(IPAddress.Parse("127.0.0.1"), 2344));//绑定端口号和IP

            if (!cmd.IsRegistered("50E58CA6-B033-4594-8F2A-B1BFD936C9A8"))
            {
                cmd.Regsvr32(AppDomain.CurrentDomain.BaseDirectory + "VCam.dll");
            }

            try
            {
                //using var mmf = MemoryMappedFile.OpenExisting("Live2dFaceTrackConfig");
            }
            catch
            {
                new MessageWindow("虚拟摄像头未安装");
            }
        }

        public static void Register()
        {
            cmd.Regsvr32(AppDomain.CurrentDomain.BaseDirectory + "VCam.dll");
        }

        public static void UnRegister()
        {
            cmd.UnRegister(AppDomain.CurrentDomain.BaseDirectory + "VCam.dll");
        }

        public static void Set(ushort width, ushort height)
        {
            using MemoryMappedViewStream stream = mmf.CreateViewStream();
            BinaryWriter writer = new(stream);
            writer.Write(1);
            writer.Write(width);
            writer.Write(height);
        }


        public static void Send(int width, int height, byte[] data)
        {
            EndPoint point = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 6000);
            server.SendTo(data, point);
        }
    }
    public class Cmd
    {
        private string ureg = "uninstall.bat";
        private string reg = "install.bat";

        public void Regsvr32(string fileName)
        {
            string strcmd = string.Format(reg, fileName);
            Execute(strcmd);
        }

        /// <summary>
        /// 使用cmd执行命令
        /// </summary>
        /// <param name="strCmd"></param>
        /// <returns></returns>
        private void Execute(string strCmd)
        {
            ProcessStartInfo processStartInfo = new()
            {
                FileName = "cmd.exe",
                UseShellExecute = false,
                RedirectStandardOutput = false,
                CreateNoWindow = true,
                WorkingDirectory = AppContext.BaseDirectory,
                Arguments = "/c " + strCmd
            };

            Process myProcess = new()
            {
                StartInfo = processStartInfo
            };

            myProcess.Start();

            myProcess.Close();
        }

        /// <summary>
        /// COM组件是否已经被注册
        /// </summary>
        /// <param name="clsid"></param>
        /// <returns></returns>
        public bool IsRegistered(string clsid)
        {
            //参数检查
            Debug.Assert(!string.IsNullOrEmpty(clsid), "clsid 不应该为空");

            //设置返回值
            bool result = false;
            //检查方法，查找注册表是否存在指定的clsid
            string key = string.Format(@"CLSID\{{{0}}}", clsid);
            RegistryKey regKey = Registry.ClassesRoot.OpenSubKey(key);
            if (regKey != null)
            {
                result = true;
            }

            return result;
        }

        /// <summary>
        /// 反注册
        /// </summary>
        /// <param name="file"></param>
        /// <returns></returns>
        public void UnRegister(string file)
        {
            string strcmd = string.Format(ureg, file);
            Execute(strcmd);
        }
    }
}
