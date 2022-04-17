using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace L2d_Desktop;

public record ConfigObj
{
    public byte Color_R { get; set; } = 0;
    public byte Color_G { get; set; } = 255;
    public byte Color_B { get; set; } = 0;
    public ushort C_Width { get; set; } = 600;
    public ushort C_Height { get; set; } = 700;

    public ushort Port { get; set; } = 12345;

    public string Local { get; set; }

    public string ParamAngleX { get; set; }
    public string ParamAngleY { get; set; }
    public string ParamAngleZ { get; set; }

    public string ParamEyeLOpen { get; set; }
    public string ParamEyeROpen { get; set; }

    public string ParamEyeBallX { get; set; }
    public string ParamEyeBallY { get; set; }

    public string ParamBodyAngleX { get; set; }
    public string ParamBodyAngleY { get; set; }
    public string ParamBodyAngleZ { get; set; }
    public string ParamBreath { get; set; }
    public string ParamMouthOpenY { get; set; }
}
