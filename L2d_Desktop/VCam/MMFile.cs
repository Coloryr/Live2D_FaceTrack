using System;
using System.Collections.Generic;
using System.IO;
using System.IO.MemoryMappedFiles;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace VCam
{
    internal class MMFile
    {
        public byte[] temp;
        public bool haveData;
        private VCamFilter cam;
        public MMFile(VCamFilter cam)
        {
            this.cam = cam;
        }

        public void Tick()
        {
            try
            {
                var mmf = MemoryMappedFile.OpenExisting("Live2dFaceTrackConfig");
                MemoryMappedViewStream stream = mmf.CreateViewStream();
                BinaryReader reader = new BinaryReader(stream);
                var load = reader.ReadInt32();
                if (load == 1)
                {
                    ushort width = reader.ReadUInt16();
                    ushort height = reader.ReadUInt16();

                    cam.Set(width, height);
                }
                stream.Dispose();
                mmf.Dispose();
            }
            catch (FileNotFoundException e)
            {

            }
            catch (Exception e)
            {
                MessageBox.Show(e.ToString());
            }

            try
            {
                var mmf = MemoryMappedFile.OpenExisting("Live2dFaceTrack");
                MemoryMappedViewStream stream = mmf.CreateViewStream();
                BinaryReader reader = new BinaryReader(stream);
                int width = reader.ReadInt32();
                int height = reader.ReadInt32();
                int size = width * height * 4;
                if (temp == null || temp.Length != size)
                    temp = new byte[size];
                reader.Read(temp, 0, temp.Length);
                haveData = true;
                stream.Dispose();
                mmf.Dispose();
            }
            catch (FileNotFoundException e)
            { 
                
            }
            catch (Exception e)
            {
                MessageBox.Show(e.ToString());
            }
        }
    }
}
