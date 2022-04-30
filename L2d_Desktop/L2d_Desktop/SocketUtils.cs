using DotNetty.Buffers;
using DotNetty.Transport.Bootstrapping;
using DotNetty.Transport.Channels;
using DotNetty.Transport.Channels.Sockets;
using System;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using DotNetty.Codecs;

namespace L2d_Desktop
{
    public record ValueSave
    {
        public static float ParamAngleX;
        public static float ParamAngleY;
        public static float ParamAngleZ;
        public static float ParamEyeLOpen;
        public static float ParamEyeROpen;
        public static float ParamEyeBallX;
        public static float ParamEyeBallY;
        public static float ParamMouthOpenY;
        public static float ParamBodyAngleZ;
        public static float ParamBodyAngleY;
    }
    internal class SocketUtils
    {
        public static byte[] PackInit = Encoding.UTF8.GetBytes("Live2D server init");
        public const string PackOk = "Live2D server ok";

        public static bool CheckOk;

        private static MultithreadEventLoopGroup group = new MultithreadEventLoopGroup();
        public static IChannel clientChannel;

        public static Task<bool> Init()
        {
            return Task.Run(async () =>
            {
                try
                {
                    var bootstrap = new Bootstrap();
                    bootstrap
                        .Group(group)
                        .Channel<TcpSocketChannel>()
                        .Handler(new ActionChannelInitializer<ISocketChannel>(channel =>
                        {
                            IChannelPipeline pipeline = channel.Pipeline;
                            pipeline.AddLast(new LengthFieldBasedFrameDecoder(1024 * 10, 0, 4, 0, 4)).AddLast(new ClientHandler());
                        }));
                    clientChannel = await bootstrap.ConnectAsync(new IPEndPoint(IPAddress.Parse("127.0.0.1"), MainWindow.Port));
                    SendTest();
                    Thread.Sleep(5000);
                    return CheckOk;
                }
                catch (Exception e)
                {
                    App.ThisApp.AddLog(e.ToString());
                }
                return false;
            });
        }

        public static async void Disconnect()
        {
            CheckOk = false;
            if (clientChannel != null)
            {
                await clientChannel.CloseAsync();
            }
        }

        public static async void Close()
        {
            await clientChannel.CloseAsync();
            await group.ShutdownGracefullyAsync();
        }

        private static void SendTest() 
        {
            clientChannel.WriteAndFlushAsync(Unpooled.CopiedBuffer(PackInit));
        }

        public static void OnPack(IByteBuffer buff)
        {
            if (!CheckOk)
            {
                string data = buff.ToString(Encoding.UTF8);
                if (data == PackOk)
                {
                    CheckOk = true;
                }
            }
            else
            {
                short type = buff.ReadShort();
                switch (type)
                {
                    case 0:
                        ValueSave.ParamAngleY = buff.ReadFloat();
                        ValueSave.ParamAngleX = buff.ReadFloat();
                        ValueSave.ParamAngleZ = buff.ReadFloat();
                        ValueSave.ParamMouthOpenY = buff.ReadFloat();
                        ValueSave.ParamEyeBallX = buff.ReadFloat();
                        ValueSave.ParamEyeBallY = buff.ReadFloat();
                        ValueSave.ParamBodyAngleZ = buff.ReadFloat();
                        ValueSave.ParamBodyAngleY = buff.ReadFloat();
                        ValueSave.ParamEyeLOpen = buff.ReadFloat();
                        ValueSave.ParamEyeROpen = buff.ReadFloat();
                        break;
                }
                buff.Clear();
            }
        }

        public static void GetModelInfo()
        {
            Task.Run(() =>
            {
                var buff = Unpooled.Buffer()
                        .WriteShort(0);
                clientChannel.WriteAndFlushAsync(buff);
            });
        }
    }
    public class ClientHandler : SimpleChannelInboundHandler<IByteBuffer>
    {
        public static int i = 0;
        /// <summary>
        /// Read0是DotNetty特有的对于Read方法的封装
        /// 封装实现了：
        /// 1. 返回的message的泛型实现
        /// 2. 丢弃非该指定泛型的信息
        /// </summary>
        /// <param name="ctx"></param>
        /// <param name="msg"></param>
        protected override void ChannelRead0(IChannelHandlerContext ctx, IByteBuffer msg)
        {
            if (msg != null)
            {
                SocketUtils.OnPack(msg);
            }
        }
        public override void ChannelReadComplete(IChannelHandlerContext context)
        {
            context.Flush();
        }
        public override void HandlerRemoved(IChannelHandlerContext context)
        {
            base.HandlerRemoved(context);
            SocketUtils.Disconnect();
            MainWindow.Disconnect();
        }

        public override void ChannelActive(IChannelHandlerContext context)
        {
            
        }

        public override void ExceptionCaught(IChannelHandlerContext context, Exception exception)
        {
            App.ThisApp.AddLog(exception);
            context.CloseAsync();
        }
    }
}
