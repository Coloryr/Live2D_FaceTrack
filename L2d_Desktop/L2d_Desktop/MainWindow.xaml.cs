using L2d_Desktop.objs;
using SharpAdbClient;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace L2d_Desktop
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public ushort C_Width { get; set; } = 540;
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

        public static void ShowModelInfo(ModelObj obj) 
        {
            if (obj == null)
                return;
            main.Dispatcher.Invoke(() =>
            {
                main.ModelName.Text = obj.name;
                main.ModelLoad.Text = obj.isLoad.ToString();
            });
        }

        private static int save;
        private static Bitmap bmp;
        private static WriteableBitmap bitmapSource;
        private static byte[] rgbvalues;
        private static MemoryStream imgstream;

        public static Bitmap BGR24ToBitmap(byte[] imgBGR, int width, int height)
        {
            if (bmp == null || bmp.Width != width || bmp.Height != height)
            {
                VCamera.Set((ushort)width, (ushort)height);
                bmp = new(width, height, System.Drawing.Imaging.PixelFormat.Format32bppArgb);
                main.Dispatcher.Invoke(() =>
                {
                    bitmapSource = new WriteableBitmap(width, height, 96, 96, PixelFormats.Bgra32, null);
                    main.ShowImg.Source = bitmapSource;
                });
            }

            if (imgBGR != null)
            {
                //构造一个位图数组进行数据存储
                if (rgbvalues == null || rgbvalues.Length != imgBGR.Length)
                    rgbvalues = new byte[imgBGR.Length];

                //对每一个像素的颜色进行转化
                for (int i = 0; i < rgbvalues.Length; i += 4)
                {
                    //rgbvalues[i] = imgBGR[^((i + 1) + 1)];
                    //rgbvalues[i + 1] = imgBGR[^((i + 2) + 1)];
                    //rgbvalues[i + 2] = imgBGR[^((i + 3) + 1)];
                    //rgbvalues[i + 3] = imgBGR[^((i + 0) + 1)];
                    rgbvalues[i] = imgBGR[i + 2];
                    rgbvalues[i + 1] = imgBGR[i + 1];
                    rgbvalues[i + 2] = imgBGR[i + 0];
                    rgbvalues[i + 3] = imgBGR[i + 3];
                }

                //以可读写的方式将图像数据锁定
                BitmapData bmpdata = bmp.LockBits(new(0, 0, bmp.Width, bmp.Height), ImageLockMode.ReadWrite, bmp.PixelFormat);
                //得到图形在内存中的首地址
                IntPtr ptr = bmpdata.Scan0;

                //将被锁定的位图数据复制到该数组内
                //Marshal.Copy(ptr, rgbvalues, 0, imgBGR.Length);
                //把处理后的图像数组复制回图像
                Marshal.Copy(rgbvalues, 0, ptr, rgbvalues.Length);

                VCamera.Send(width, height, rgbvalues);

                main.Dispatcher.Invoke(() => 
                { 
                    bitmapSource.Lock(); 
                    Marshal.Copy(rgbvalues, 0, bitmapSource.BackBuffer, rgbvalues.Length); //请注意_wbBitmap的数据格式以及buffer大小，以免溢出和显示异常
                    bitmapSource.AddDirtyRect(new Int32Rect(0, 0, width, height));
                    bitmapSource.Unlock();
                });


                //解锁位图像素
                bmp.UnlockBits(bmpdata);

            }

            return bmp;
        }

        public static void ShowPic(byte[] data, int width, int height)
        {
            save++;
            var bitmap = BGR24ToBitmap(data, width, height);
            if (save > 10)
            {
                save = 0;

                bitmap.Save("test.png");
            }
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            AdbUtils.Start();
            ResDevices();
            VCamera.Test();
            VCamera.Set(C_Width, C_Height);
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
            SocketUtils.GetModelInfo();
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
            VCamera.Set(C_Width, C_Height);
        }
    }
}
