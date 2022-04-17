using OpenTK.Graphics.OpenGL;
using OpenTK.Windowing.Common;
using OpenTK.WinForms;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using System.Windows.Resources;

namespace L2d_Desktop
{
    /// <summary>
    /// GLWindow.xaml 的交互逻辑
    /// </summary>
    public partial class GLWindow : Window
    {
        public static GLWindow window;
        public Live2dApp live2d;
        public string[] Expressions;
        public Motion[] Motions;
        public Part[] Parts;
        public Parameter[] Parameters;

        private Timer _timer = null!;
        private GLControl glControl;
        private Random random = new();
        private DateTime beginTime = DateTime.Now;            //获取开始时间  

        private byte[] data;
        private byte[] rgbvalues;

        public GLWindow()
        {
            InitializeComponent();
            window = this;
            live2d = new(LoadFile, LoadDone, Update);
        }

        public void BGR24ToBitmap()
        {
            if (data != null)
            {
                //构造一个位图数组进行数据存储
                if (rgbvalues == null || rgbvalues.Length != data.Length)
                    rgbvalues = new byte[data.Length];

                //对每一个像素的颜色进行转化
                for (int i = 0; i < rgbvalues.Length; i += 4)
                {
                    //rgbvalues[i] = imgBGR[^((i + 1) + 1)];
                    //rgbvalues[i + 1] = imgBGR[^((i + 2) + 1)];
                    //rgbvalues[i + 2] = imgBGR[^((i + 3) + 1)];
                    //rgbvalues[i + 3] = imgBGR[^((i + 0) + 1)];

                    rgbvalues[i + 3] = data[i + 3];

                    if (rgbvalues[i + 3] == 0)
                    {
                        rgbvalues[i + 3] = 255;
                        rgbvalues[i + 2] = MainWindow.Color_R;
                        rgbvalues[i + 1] = MainWindow.Color_G;
                        rgbvalues[i + 0] = MainWindow.Color_B;
                    }
                    else
                    {
                        rgbvalues[i + 2] = data[i + 0];
                        rgbvalues[i + 1] = data[i + 1];
                        rgbvalues[i] = data[i + 2];
                    }
                }

                VCamera.Send(glControl.Width, glControl.Height, rgbvalues);
            }
        }

        private void Update()
        {
            
        }

        private void LoadDone(string name)
        {
            live2d.SetRandomMotion(false);
            live2d.SetCustomValue(true);

            Expressions = live2d.GetExpressions();
            Motions = live2d.GetMotions();
            Parts = live2d.GetParts();
            Parameters = live2d.GetParameters();
        }

        private IntPtr LoadFile(string path, ref uint size)
        {
            var temp = File.ReadAllBytes(path);
            IntPtr inputBuffer = Marshal.AllocHGlobal(temp.Length * sizeof(byte));
            Marshal.Copy(temp, 0, inputBuffer, temp.Length);
            size = (uint)temp.Length;
            return inputBuffer;
        }

        internal void SetSize(ushort c_Width, ushort c_Height)
        {
            Width = c_Width;
            Height = c_Height;

            data = new byte[glControl.Width * glControl.Height * 4];

            VCamera.Test();
            VCamera.Set((ushort)glControl.Width, (ushort)glControl.Height);
        }

        private void GlControl_Load(object? sender, EventArgs e)
        {
            glControl.Paint += glControl_Paint;

            _timer = new Timer();
            _timer.Tick += (sender, e) =>
            {
                Render();
            };
            _timer.Interval = 10;   // 1000 ms per sec / 10 ms per frame = 100 FPS
            _timer.Start();

            live2d.Start(glControl.ClientSize.Width, glControl.ClientSize.Height);
            live2d.LoadModel(@"E:\live2d\haru\runtime/", "Haru");
        }

        private void glControl_Paint(object? sender, PaintEventArgs e)
        {
            Render();
        }

        private void Render()
        {
            glControl.MakeCurrent();

            live2d.Tick(glControl.Width, glControl.Height, (DateTime.Now - beginTime).TotalMilliseconds / 1000);

            glControl.SwapBuffers();

            GL.ReadPixels(0, 0, glControl.Width, glControl.Height, PixelFormat.Rgba, PixelType.Byte, data);
            BGR24ToBitmap();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            var set = GLControlSettings.Default.Clone();
            set.API = ContextAPI.OpenGL;
            set.Flags = ContextFlags.Default;
            set.IsEventDriven = true;
            set.Profile = ContextProfile.Compatability;
            set.APIVersion = new Version(3, 3, 0, 0);
            glControl = new GLControl(set);//创建GLControl控件

            glControl.Load += GlControl_Load;
            Host.Child = glControl;//将控件放在WindowsFormsHost控件中
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
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
