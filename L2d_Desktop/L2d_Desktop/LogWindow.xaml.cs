using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace L2d_Desktop
{
    /// <summary>
    /// LogWindow.xaml 的交互逻辑
    /// </summary>
    public partial class LogWindow : Window
    {
        public LogWindow()
        {
            InitializeComponent();
        }

        public void AddLog(string data) 
        {
            Dispatcher.Invoke(() =>
            {
                Log.AppendText(data + Environment.NewLine);
                Log.ScrollToEnd();
            });
        }

        private void Window_Closing(object sender, CancelEventArgs e)
        {
            var res = new SelectWindow("是否要关闭日志窗口").Set();
            if (res)
            {
                App.CloseLog();
            }
            else
            {
                e.Cancel = true;
            }
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            Log.Text = "";
        }
    }
}
