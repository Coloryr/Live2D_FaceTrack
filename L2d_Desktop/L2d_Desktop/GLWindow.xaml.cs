using Live2DCSharpSDK.App;
using OpenTK.Graphics.OpenGL4;
using OpenTK.Windowing.Common;
using OpenTK.Windowing.Desktop;
using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows.Forms;
using System.Xml.Linq;
using ErrorCode = OpenTK.Graphics.OpenGL4.ErrorCode;

namespace L2d_Desktop;

public class GLWindow : GameWindow
{
    public static GLWindow window;
    private LAppDelegate lapp;
    public List<string> Expressions;
    public List<string> Motions;
    public List<(string, int, float)> Parts;
    public List<string> Parameters;

    private LAppModel _model;
    private Timer _timer = null!;
    private Random random = new();
    private DateTime beginTime = DateTime.Now;            //获取开始时间  

    private byte[] data;
    private byte[] rgbvalues;

    private int ParamAngleX =-1;
    private int ParamAngleY = -1;
    private int ParamAngleZ = -1;
    private int ParamEyeLOpen = -1;
    private int ParamEyeROpen = -1;
    private int ParamEyeBallX = -1;
    private int ParamEyeBallY = -1;
    private int ParamMouthOpenY = -1;
    private int ParamBodyAngleY = -1;
    private int ParamBodyAngleZ = -1;

    public GLWindow(GameWindowSettings gameWindowSettings, NativeWindowSettings nativeWindowSettings)
            : base(gameWindowSettings, nativeWindowSettings)
    {
        window = this;
        var version = GL.GetString(StringName.Version);

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
                rgbvalues[i + 3] = data[i + 3];
                rgbvalues[i + 2] = data[i + 0];
                rgbvalues[i + 1] = data[i + 1];
                rgbvalues[i] = data[i + 2];
            }

            VCamera.Send(ClientSize.X, ClientSize.Y, rgbvalues);
        }
    }

    public void CheckIndex() 
    {
        foreach (var item in Parts)
        {
            var name = item.Item1;
            if (name == App.Config.ParamAngleX)
            {
                ParamAngleX = item.Item2;
            }
            if (name == App.Config.ParamAngleY)
            {
                ParamAngleY = item.Item2;
            }
            if (name == App.Config.ParamAngleZ)
            {
                ParamAngleZ = item.Item2;
            }
            if (name == App.Config.ParamEyeLOpen)
            {
                ParamEyeLOpen = item.Item2;
            }
            if (name == App.Config.ParamEyeROpen)
            {
                ParamEyeROpen = item.Item2;
            }
            if (name == App.Config.ParamEyeBallX)
            {
                ParamEyeBallX = item.Item2;
            }
            if (name == App.Config.ParamEyeBallY)
            {
                ParamEyeBallY = item.Item2;
            }
            if (name == App.Config.ParamMouthOpenY)
            {
                ParamMouthOpenY = item.Item2;
            }
            if (name == App.Config.ParamBodyAngleY)
            {
                ParamBodyAngleY = item.Item2;
            }
            if (name == App.Config.ParamBodyAngleZ)
            {
                ParamBodyAngleZ = item.Item2;
            }
        }
    }

    private void Update(LAppModel model)
    {
        model.Model.AddParameterValue(ParamAngleX, ValueSave.ParamAngleX);
        model.Model.AddParameterValue(ParamAngleY, ValueSave.ParamAngleY);
        model.Model.AddParameterValue(ParamAngleZ, ValueSave.ParamAngleZ);
        model.Model.AddParameterValue(ParamEyeLOpen, ValueSave.ParamEyeLOpen);
        model.Model.AddParameterValue(ParamEyeROpen, ValueSave.ParamEyeROpen);
        model.Model.AddParameterValue(ParamEyeBallX, ValueSave.ParamEyeBallX);
        model.Model.AddParameterValue(ParamEyeBallY, ValueSave.ParamEyeBallY);
        model.Model.AddParameterValue(ParamMouthOpenY, ValueSave.ParamMouthOpenY);
        model.Model.AddParameterValue(ParamBodyAngleZ, ValueSave.ParamBodyAngleZ);
        model.Model.AddParameterValue(ParamBodyAngleY, ValueSave.ParamBodyAngleY);
    }

    protected unsafe override void OnLoad()
    {
        base.OnLoad();
        lapp = new(new OpenTKApi(this), Console.WriteLine);

        if (File.Exists(App.Config.Local) && App.Config.Local.EndsWith(".model3.json"))
        {
            FileInfo info = new(App.Config.Local);
            LoadModel(info.DirectoryName + "/", info.Name);
        }
    }

    internal void LoadModel(string v, string name)
    {
        if (_model != null)
        {
            lapp.Live2dManager.ReleaseAllModel();   
        }
        _model = lapp.Live2dManager.LoadModel(v, name.Replace(".model3.json", ""));

        _model.RandomMotion = false;
        _model.CustomValueUpdate = true;

        Expressions = _model.Expressions;
        Motions = _model.Motions;
        Parts = _model.Parts;
        Parameters = _model.Parameters;

        _model.ValueUpdate = Update;

        MainWindow.main.LoadDone();
    }

    protected override void OnRenderFrame(FrameEventArgs e)
    {
        base.OnRenderFrame(e);

        GL.ClearColor((float)App.Config.Color_R / 255, (float)App.Config.Color_G / 255, (float)App.Config.Color_B / 255, 1.0f);
        GL.Clear(ClearBufferMask.ColorBufferBit | ClearBufferMask.DepthBufferBit);
        GL.ClearDepth(1.0);

        lapp.Run((float)RenderTime);

        GL.ReadPixels(0, 0, ClientSize.X, ClientSize.Y, PixelFormat.Rgba, PixelType.UnsignedByte, data);
        BGR24ToBitmap();

        var code = GL.GetError();
        if (code != ErrorCode.NoError)
        {
            throw new Exception();
        }

        SwapBuffers();
    }

    protected override void OnResize(ResizeEventArgs e)
    {
        base.OnResize(e);

        lapp.Resize();

        data = new byte[e.Width * e.Height * 4];

        VCamera.Set((ushort)e.Width, (ushort)e.Height);

        GL.Viewport(0, 0, e.Width, e.Height);
    }

    protected override void OnUpdateFrame(FrameEventArgs e)
    {
        base.OnUpdateFrame(e);
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

    internal void SetSize(ushort c_Width, ushort c_Height)
    {
        Size = new(c_Width, c_Height);
    }

    internal void InitBreath()
    {
        _model.LoadBreath();
    }

    internal void SetIdParamAngleX(string paramAngleX)
    {
        _model.IdParamAngleX = paramAngleX;
    }

    internal void SetIdParamAngleY(string paramAngleY)
    {
        _model.IdParamAngleY = paramAngleY;
    }

    internal void SetIdParamAngleZ(string item)
    {
        _model.IdParamAngleZ = item;
    }

    internal void SetIdParamBodyAngleX(string item)
    {
        _model.IdParamBodyAngleX = item;
    }

    internal void SetIdParamEyeBallX(string paramEyeBallX)
    {
        _model.IdParamEyeBallX = paramEyeBallX;
    }

    internal void SetIdParamEyeBallY(string paramEyeBallY)
    {
        _model.IdParamEyeBallY = paramEyeBallY;
    }

    internal void SetIdParamBreath(string paramBreath)
    {
        _model.IdParamBreath = paramBreath;
    }

    internal void SetPartOpacitie(string id, float value)
    {
        _model.Model.SetPartOpacity(id, value);
    }
}
