using OpenTK.Windowing.Common;
using OpenTK.Windowing.Desktop;
using System;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;

namespace L2d_Desktop
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        public const string Version = "1.0.0";

        public static App ThisApp { get; private set; }
        public static string Local { get; private set; }

        private static LogWindow log;

        public App()
        {
            ThisApp = this;
            Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
        }

        public void AddLog(Exception data)
        {
            if (log == null)
            {
                Dispatcher.Invoke(() =>
                {
                    log = new();
                    log.Show();
                });
            }

            log.AddLog(data.ToString());
        }

        public void AddLog(string data) 
        {
            if (log == null) 
            {
                Dispatcher.Invoke(() =>
                {
                    log = new();
                    log.Show();
                });
            }

            log.AddLog(data);
        }

        private void Application_Startup(object sender, StartupEventArgs e)
        {
            ThisApp = this;
            Local = AppDomain.CurrentDomain.BaseDirectory;

            log = new();
            log.Show();

            DispatcherUnhandledException += new DispatcherUnhandledExceptionEventHandler(App_DispatcherUnhandledException);
            TaskScheduler.UnobservedTaskException += TaskScheduler_UnobservedTaskException;
            AppDomain.CurrentDomain.UnhandledException += new UnhandledExceptionEventHandler(CurrentDomain_UnhandledException);
        }

        public static void CloseLog()
        {
            log = null;
        }

        private void App_DispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            try
            {
                e.Handled = true;
                MessageBox.Show("捕获未处理异常:" + e.Exception.ToString());
            }
            catch (Exception ex)
            {
                MessageBox.Show("发生错误" + ex.ToString());
            }

        }

        private void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            StringBuilder sbEx = new StringBuilder();
            if (e.IsTerminating)
            {
                sbEx.Append("发生错误，将关闭\n");
            }
            sbEx.Append("捕获未处理异常：");
            if (e.ExceptionObject is Exception)
            {
                sbEx.Append(((Exception)e.ExceptionObject).ToString());
            }
            else
            {
                sbEx.Append(e.ExceptionObject);
            }
            MessageBox.Show(sbEx.ToString());
        }

        private void TaskScheduler_UnobservedTaskException(object? sender, UnobservedTaskExceptionEventArgs e)
        {
            MessageBox.Show("捕获线程内未处理异常：" + e.Exception.ToString());
            e.SetObserved();
        }
    }
}
