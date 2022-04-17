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
    internal class SocketUtils
    {
        public static byte[] PackInit = Encoding.UTF8.GetBytes("Live2D server init");
        public const string PackOk = "Live2D server ok";

        public static bool CheckOk;
        public static Semaphore Semaphore;

        private static MultithreadEventLoopGroup group = new MultithreadEventLoopGroup();
        public static IChannel clientChannel;
        private static IByteBuffer delimiter = Unpooled.Buffer();

        public static Task<bool> Init()
        {
            return Task.Run(async () =>
            {
                try
                {
                    Semaphore = new(0, 5);
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
                    Semaphore.WaitOne(TimeSpan.FromSeconds(10));
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
            Semaphore.Release();
            if (clientChannel != null)
            {
                await clientChannel.CloseAsync();
            }
        }

        public static async void Close()
        {
            Semaphore.Dispose();
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
                Semaphore.Release();
            }
            else
            {
                short type = buff.ReadShort();
                switch (type)
                {
                    
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
