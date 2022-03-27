using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace L2d_Desktop
{
    internal class RgbaBitmapSource : BitmapSource
    {
        private Bitmap bitmap;
        private static byte[] imgBGR;

        public RgbaBitmapSource(Bitmap bitmap)
        {
            this.bitmap = bitmap;
            imgBGR = new byte[bitmap.Width * bitmap.Height * 4];
        }

        public override void CopyPixels(
            Int32Rect sourceRect, Array pixels, int stride, int offset)
        {
            //位图矩形
            Rectangle rect = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
            //以可读写的方式将图像数据锁定
            System.Drawing.Imaging.BitmapData bmpdata = bitmap.LockBits(rect, System.Drawing.Imaging.ImageLockMode.ReadWrite, bitmap.PixelFormat);
            //得到图形在内存中的首地址
            IntPtr ptr = bmpdata.Scan0;

            //将被锁定的位图数据复制到该数组内
            Marshal.Copy(ptr, imgBGR, 0, imgBGR.Length);
            //解锁位图像素
            bitmap.UnlockBits(bmpdata);

            for (int y = sourceRect.Y; y < sourceRect.Y + sourceRect.Height; y++)
            {
                for (int x = sourceRect.X; x < sourceRect.X + sourceRect.Width; x++)
                {
                    int i = stride * y + 4 * x;
                    byte a = imgBGR[i + 3];
                    byte r = (byte)(imgBGR[i] * a / 256); // pre-multiplied R
                    byte g = (byte)(imgBGR[i + 1] * a / 256); // pre-multiplied G
                    byte b = (byte)(imgBGR[i + 2] * a / 256); // pre-multiplied B

                    pixels.SetValue(b, i + offset);
                    pixels.SetValue(g, i + offset + 1);
                    pixels.SetValue(r, i + offset + 2);
                    pixels.SetValue(a, i + offset + 3);
                }
            }
        }

        protected override Freezable CreateInstanceCore()
        {
            return new RgbaBitmapSource(bitmap);
        }

        public override event EventHandler<DownloadProgressEventArgs> DownloadProgress;
        public override event EventHandler DownloadCompleted;
        public override event EventHandler<ExceptionEventArgs> DownloadFailed;
        public override event EventHandler<ExceptionEventArgs> DecodeFailed;

        public override double DpiX
        {
            get { return 96; }
        }

        public override double DpiY
        {
            get { return 96; }
        }

        public override PixelFormat Format
        {
            get { return PixelFormats.Pbgra32; }
        }

        public override int PixelWidth
        {
            get { return bitmap.Width; }
        }

        public override int PixelHeight
        {
            get { return bitmap.Height; }
        }

        public override double Width
        {
            get { return bitmap.Width; }
        }

        public override double Height
        {
            get { return bitmap.Height; }
        }
    }
}
