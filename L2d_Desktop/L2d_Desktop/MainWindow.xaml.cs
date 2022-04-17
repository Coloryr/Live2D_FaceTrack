using Microsoft.Win32;
using SharpAdbClient;
using System.IO;
using System.Windows;

namespace L2d_Desktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public static byte Color_R { get; set; } = 0;
        public static byte Color_G { get; set; } = 255;
        public static byte Color_B { get; set; } = 0;
        public ushort C_Width { get; set; } = 600;
        public ushort C_Height { get; set; } = 600;

        private bool isDo;
        private bool isConnect;
        private static MainWindow main;

        public static ushort Port { get; set; } = 12580;
        public MainWindow()
        {
            main = this;
            InitializeComponent();
            DataContext = this;
        }

        public static void Disconnect() 
        {
            App.ThisApp.AddLog("手机链接中断");
            main.Dispatcher.Invoke(() =>
            {
                main.Dis();
            });
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            new GLWindow().Show();

            GLWindow.window.SetSize(C_Width, C_Height);
            AdbUtils.Start();
            ResDevices();
        }

        private void ResDevices() 
        {
            var list = AdbUtils.GetDevices();
            DeviceList.Items.Clear();
            foreach (var item in list) 
            {
                DeviceList.Items.Add(item);
            }
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            ResDevices();
        }

        private void Dis() 
        {
            isConnect = false;
            Group_Model.IsEnabled = false;
            Group_Setting.IsEnabled = true;
            DeviceList.IsEnabled = true;
            Res_Button.IsEnabled = true;
            ResDevices();
            Conn_Button.Content = "链接";
        }

        private async void Button_Click_1(object sender, RoutedEventArgs e)
        {
            if (!isConnect)
            {
                if (isDo)
                    return;
                var item = DeviceList.SelectedItem;
                if (item == null)
                {
                    new MessageWindow("你没有选择设备");
                    return;
                }

                var device = DeviceList.SelectedItem as DeviceData;
                if (device == null)
                {
                    new MessageWindow("内部错误");
                    return;
                }
                App.ThisApp.AddLog($"正在尝试链接手机{device.Name}");
                isDo = true;
                await AdbUtils.Connect(device);
                var res = await SocketUtils.Init();
                if (!res)
                {
                    App.ThisApp.AddLog($"手机连接失败{device.Name}");
                    new MessageWindow("手机连接失败");
                    isDo = false;
                    return;
                }

                isConnect = true;
                Group_Model.IsEnabled = true;
                Group_Setting.IsEnabled = false;
                DeviceList.IsEnabled = false;
                Res_Button.IsEnabled = false;
                Conn_Button.Content = "断开链接";
                isDo = false;
                App.ThisApp.AddLog($"连接成功{device.Name}");

                SocketUtils.GetModelInfo();
            }
            else
            {
                SocketUtils.Disconnect();
                Dis();
            }
        }

        private void Button_Click_2(object sender, RoutedEventArgs e)
        {
            OpenFileDialog file = new()
            {
                Filter = "模型文件|*.model3.json",
                RestoreDirectory = true,
                FilterIndex = 1
            };

            if (file.ShowDialog() == true)
            {
                string filename = file.FileName;
                FileInfo info = new(filename);

                GLWindow.window.live2d.LoadModel(info.DirectoryName + "/", info.Name.Replace(".model3.json", ""));
            }
        }

        private void Button_Click_3(object sender, RoutedEventArgs e)
        {
            VCamera.Register();
        }

        private void Button_Click_4(object sender, RoutedEventArgs e)
        {
            VCamera.UnRegister();
        }

        private void Button_Click_5(object sender, RoutedEventArgs e)
        {
            GLWindow.window.SetSize(C_Width, C_Height);
        }
    }
}
