using Microsoft.Win32;
using SharpAdbClient;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;

namespace L2d_Desktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        private bool isDo;
        private bool isConnect;
        public static MainWindow main;

        public class PartOb : INotifyPropertyChanged
        {
            private float _Opacitie;

            public string Id { get; set; }
            public float Opacitie
            {
                get
                {
                    return _Opacitie;
                }
                set
                {
                    _Opacitie = value;
                    OnPropertyChanged();
                }
            }

            public event PropertyChangedEventHandler? PropertyChanged;

            private void OnPropertyChanged([CallerMemberName] string? propertyName = null)
            {
                PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            }
        }

        public static ushort Port { get; set; } = 12580;
        public MainWindow()
        {
            main = this;
            InitializeComponent();
            DataContext = App.Config;
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
            VCamera.Test();
            GLWindow.window.SetSize(App.Config.C_Width, App.Config.C_Height);
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
                App.Config.Local = filename;
                App.SaveConfig();
                GLWindow.window.live2d.LoadModel(info.DirectoryName + "/", info.Name);
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
            GLWindow.window.SetSize(App.Config.C_Width, App.Config.C_Height);
            App.SaveConfig();
        }

        public void LoadDone() 
        {
            Dispatcher.Invoke(() =>
            {
                Com_ParamAngleX.SelectedItem = null;
                Com_ParamAngleY.SelectedItem = null;
                Com_ParamAngleZ.SelectedItem = null;
                Com_ParamEyeLOpen.SelectedItem = null;
                Com_ParamEyeROpen.SelectedItem = null;
                Com_ParamEyeBallX.SelectedItem = null;
                Com_ParamEyeBallY.SelectedItem = null;
                Com_ParamBodyAngleX.SelectedItem = null;
                Com_ParamBodyAngleY.SelectedItem = null;
                Com_ParamBodyAngleZ.SelectedItem = null;
                Com_ParamBreath.SelectedItem = null;
                Com_ParamMouthOpenY.SelectedItem = null;

                Com_ParamAngleX.Items.Clear();
                Com_ParamAngleY.Items.Clear();
                Com_ParamAngleZ.Items.Clear();
                Com_ParamEyeLOpen.Items.Clear();
                Com_ParamEyeROpen.Items.Clear();
                Com_ParamEyeBallX.Items.Clear();
                Com_ParamEyeBallY.Items.Clear();
                Com_ParamBodyAngleX.Items.Clear();
                Com_ParamBodyAngleY.Items.Clear();
                Com_ParamBodyAngleZ.Items.Clear();
                Com_ParamBreath.Items.Clear();
                Com_ParamMouthOpenY.Items.Clear();

                Com_ParamAngleX.Items.Add("");
                Com_ParamAngleY.Items.Add("");
                Com_ParamAngleZ.Items.Add("");
                Com_ParamEyeLOpen.Items.Add("");
                Com_ParamEyeROpen.Items.Add("");
                Com_ParamEyeBallX.Items.Add("");
                Com_ParamEyeBallY.Items.Add("");
                Com_ParamBodyAngleX.Items.Add("");
                Com_ParamBodyAngleY.Items.Add("");
                Com_ParamBodyAngleZ.Items.Add("");
                Com_ParamBreath.Items.Add("");
                Com_ParamMouthOpenY.Items.Add("");

                if (GLWindow.window.Parameters == null)
                    return;

                foreach (var item in GLWindow.window.Parameters)
                {
                    Com_ParamAngleX.Items.Add(item.Id);
                    Com_ParamAngleY.Items.Add(item.Id);
                    Com_ParamAngleZ.Items.Add(item.Id);
                    Com_ParamEyeLOpen.Items.Add(item.Id);
                    Com_ParamEyeROpen.Items.Add(item.Id);
                    Com_ParamEyeBallX.Items.Add(item.Id);
                    Com_ParamEyeBallY.Items.Add(item.Id);
                    Com_ParamBodyAngleX.Items.Add(item.Id);
                    Com_ParamBodyAngleY.Items.Add(item.Id);
                    Com_ParamBodyAngleZ.Items.Add(item.Id);
                    Com_ParamBreath.Items.Add(item.Id);
                    Com_ParamMouthOpenY.Items.Add(item.Id);
                }

                if (Com_ParamAngleX.Items.Contains(App.Config.ParamAngleX))
                {
                    Com_ParamAngleX.SelectedItem = App.Config.ParamAngleX;
                    GLWindow.window.live2d.SetIdParamAngleX(App.Config.ParamAngleX);
                }
                if (Com_ParamAngleY.Items.Contains(App.Config.ParamAngleY))
                {
                    Com_ParamAngleY.SelectedItem = App.Config.ParamAngleY;
                    GLWindow.window.live2d.SetIdParamAngleY(App.Config.ParamAngleY);
                }
                if (Com_ParamAngleZ.Items.Contains(App.Config.ParamAngleZ))
                {
                    Com_ParamAngleZ.SelectedItem = App.Config.ParamAngleZ;
                    GLWindow.window.live2d.SetIdParamAngleZ(App.Config.ParamAngleZ);
                }
                if (Com_ParamEyeLOpen.Items.Contains(App.Config.ParamEyeLOpen))
                {
                    Com_ParamEyeLOpen.SelectedItem = App.Config.ParamEyeLOpen;
                }
                if (Com_ParamEyeROpen.Items.Contains(App.Config.ParamEyeROpen))
                {
                    Com_ParamEyeROpen.SelectedItem = App.Config.ParamEyeROpen;
                }
                if (Com_ParamEyeBallX.Items.Contains(App.Config.ParamEyeBallX))
                {
                    Com_ParamEyeBallX.SelectedItem = App.Config.ParamEyeBallX;
                    GLWindow.window.live2d.SetIdParamEyeBallX(App.Config.ParamEyeBallX);
                }
                if (Com_ParamEyeBallY.Items.Contains(App.Config.ParamEyeBallY))
                {
                    Com_ParamEyeBallY.SelectedItem = App.Config.ParamEyeBallY;
                    GLWindow.window.live2d.SetIdParamEyeBallY(App.Config.ParamEyeBallY);
                }
                if (Com_ParamBodyAngleX.Items.Contains(App.Config.ParamBodyAngleX))
                {
                    Com_ParamBodyAngleX.SelectedItem = App.Config.ParamBodyAngleX;
                    GLWindow.window.live2d.SetIdParamBodyAngleX(App.Config.ParamBodyAngleX);
                }
                if (Com_ParamBodyAngleY.Items.Contains(App.Config.ParamBodyAngleY))
                {
                    Com_ParamBodyAngleY.SelectedItem = App.Config.ParamBodyAngleY;
                }
                if (Com_ParamBodyAngleZ.Items.Contains(App.Config.ParamBodyAngleZ))
                {
                    Com_ParamBodyAngleZ.SelectedItem = App.Config.ParamBodyAngleZ;
                }
                if (Com_ParamBreath.Items.Contains(App.Config.ParamBreath))
                {
                    Com_ParamBreath.SelectedItem = App.Config.ParamBreath;
                    GLWindow.window.live2d.SetIdParamBreath(App.Config.ParamBreath);
                }
                if (Com_ParamMouthOpenY.Items.Contains(App.Config.ParamMouthOpenY))
                {
                    Com_ParamMouthOpenY.SelectedItem = App.Config.ParamMouthOpenY;
                }
                GLWindow.window.live2d.InitBreath();
                GLWindow.window.CheckIndex();
            });
        }

        private void Com_ParamAngleX_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamAngleX.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamAngleX = item;
            GLWindow.window.live2d.SetIdParamAngleX(item);
            GLWindow.window.live2d.InitBreath();
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamAngleY_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamAngleY.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamAngleY = item;
            GLWindow.window.live2d.SetIdParamAngleY(item);
            GLWindow.window.live2d.InitBreath();
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamAngleZ_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamAngleZ.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamAngleZ = item;
            GLWindow.window.live2d.SetIdParamAngleZ(item);
            GLWindow.window.live2d.InitBreath();
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamEyeLOpen_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamEyeLOpen.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamEyeLOpen = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamEyeROpen_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamEyeROpen.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamEyeROpen = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamEyeBallX_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamEyeBallX.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamEyeBallX = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamEyeBallY_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamEyeBallY.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamEyeBallY = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamBodyAngleX_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamBodyAngleX.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamBodyAngleX = item;
            GLWindow.window.live2d.SetIdParamBodyAngleX(item);
            GLWindow.window.live2d.InitBreath();
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamBreath_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamBreath.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamBreath = item;
            GLWindow.window.live2d.SetIdParamBodyAngleX(item);
            GLWindow.window.live2d.InitBreath();
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamMouthOpenY_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamMouthOpenY.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamMouthOpenY = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamBodyAngleY_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamBodyAngleY.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamBodyAngleY = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void Com_ParamBodyAngleZ_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            var item = Com_ParamBodyAngleZ.SelectedItem as string;
            if (item == null)
                return;

            App.Config.ParamBodyAngleZ = item;
            GLWindow.window.CheckIndex();

            App.SaveConfig();
        }

        private void MenuItem_Click(object sender, RoutedEventArgs e)
        {
            var item = Part_List.SelectedItem as PartOb;
            if (item == null)
                return;

            var value = new InputBoxWindow(item.Opacitie).Set();

            GLWindow.window.live2d.SetPartOpacitie(item.Id, value);
            item.Opacitie = value;
        }

        private void MenuItem_Click1(object sender, RoutedEventArgs e) 
        {
            var list = GLWindow.window.live2d.GetParts();
            if (list == null)
            {
                return;
            }

            Part_List.Items.Clear();
            foreach (var item in list)
            {
                Part_List.Items.Add(new PartOb()
                {
                    Id = item.Id,
                    Opacitie = item.Opacitie
                });
            }
        }

        private void Window_Closing(object sender, CancelEventArgs e)
        {
            var res = new SelectWindow("是否要关闭软件").Set();
            if (res)
            {
                App.ThisApp.Shutdown();
            }
            else
            {
                e.Cancel = true;
            }
        }
    }
}
