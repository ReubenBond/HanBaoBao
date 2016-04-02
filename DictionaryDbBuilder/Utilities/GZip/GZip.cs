namespace DictionaryDbBuilder.Utilities.GZip
{
    using System;
    using System.IO;
    using System.IO.Compression;
    using System.Text;

    using DictionaryDbBuilder.Utilities.StartDict;

    /// <summary>
    ///     OS flag of gzip file, which indicates on what platform the archive is created
    /// </summary>
    public enum GzOs : byte
    {
        Fat, 

        Amiga, 

        Vms, 

        Unix, 

        VmCms, 

        Hpfs, 

        Macintosh, 

        ZSystem, 

        CpM, 

        Tops20, 

        Ntfs, 

        Qdos, 

        AcornRiscos, 

        Unknown = 255
    }

#if !NET45
    public enum CompressionLevel
    {
        Optimal, 

        Fastest, 

        NoCompression
    }
#endif

    // writing is not supported by this class
    public class GzExtraField
    {
        protected FileStream Fs;

        public virtual byte[] Data
        {
            get
            {
                if (this.IsCreating)
                {
                    return null;
                }

                var buf = new byte[this.Length];
                this.Fs.Position = this.FieldDataBegin;
                this.Fs.Read(buf, 0, this.Length);
                return buf;
            }
        }

        public virtual int FieldDataBegin => 10 + 6;

        public int Length { get; protected set; }

        public int TotalLength => this.XLength + 2;

        /// <summary>
        ///     Gets the length of the whole extra field minus 2
        ///     This value is redundant, will be computed from the Length field
        /// </summary>
        public int XLength => this.Length + 4;

        internal FileStream FileStream
        {
            get
            {
                return this.Fs;
            }

            set
            {
                this.Fs = value;
            }
        }

        internal byte[] Id { get; } = new byte[2];

        internal bool IsCreating { get; set; }

        internal virtual void Read()
        {
            var fs = this.FileStream;

            // Because the xlen field if redundant its value will be discarded
            fs.GetIntLittleEndian(2);
            this.Id[0] = (byte)fs.ReadByte();
            this.Id[1] = (byte)fs.ReadByte();
            this.Length = (int)fs.GetIntLittleEndian(2);
            fs.Position = this.TotalLength + 10;
        }
    }

    public abstract class GZipBase<T> : IDisposable
        where T : GzExtraField, new()
    {
        private const int Bufsize = 1024 * 4;

        private const int GzCm = 8;

        private const int GzId1 = 0x1f;

        private const int GzId2 = 0x8b;

        private const int Mcomment = 16;

        private const int Mextra = 4;

        private const int Mhcrc = 2;

        private const int Mname = 8;

        private static readonly DateTime UnixStartTime = new DateTime(1970, 1, 1, 0, 0, 0, 0);

        public readonly FileStream FileStream;

        public readonly FileMode OpenMode;

        protected byte[] _comment;

        protected byte[] _name;

        protected long DataBegin;

        private byte _flag;

        private int _hcrc;

        private uint _origSize;

        private T _xfield;

        private byte _xflag;

        /// <summary>
        ///     Initializes a new instance of the <see cref="DictUtil.GZipBase`1" /> class.
        /// </summary>
        /// <param name='path'>
        ///     Path of the archive
        /// </param>
        /// <param name='openMode'>
        ///     Open mode. Only Open(read) and Create are supported
        /// </param>
        protected GZipBase(string path, FileMode openMode)
        {
            this.OpenMode = openMode;
            if (openMode == FileMode.Open)
            {
                this.FileStream = File.OpenRead(path);
                if (this.FileStream.Length < 18)
                {
                    this.IsValid = false;
                    this.Dispose();
                }

                this.ReadHeader();

                // subclass may change this prop, therefore, leave it open
                this.DeflateStream = new DeflateStream(this.FileStream, CompressionMode.Decompress, true);
            }
            else if (openMode == FileMode.Create)
            {
                this.FileStream = File.Create(path);

                // move the following line to subclasses give them more flexibility
                // 				DeflateStream = new DeflateStream(FileStream, CompressionMode.Compress,true);
            }
        }

        ~GZipBase()
        {
            this.Dispose();
        }

        public string Comment
        {
            get
            {
                return this.HasComment ? this.BytesToString(this._comment) : string.Empty;
            }

            set
            {
                if (!this.IsHeaderWritten && !string.IsNullOrEmpty(value))
                {
                    this._comment = this.StringToBytes(value);
                    this._flag |= Mcomment;
                }
            }
        }

        public long CompressedDataSize => this.FileStream.Length - 8 - this.DataBegin;

        public long CompressedFileSize => this.FileStream.Length;

        public CompressionLevel CompressionLevel
            => this._xflag == 2 ? CompressionLevel.Optimal : CompressionLevel.Fastest;

        public uint Crc { get; private set; }

        // May be changed by subclasses
        public DeflateStream DeflateStream { get; protected set; }

        public T ExtraField
        {
            get
            {
                return this._xfield;
            }

            protected set
            {
                this._flag |= Mextra;
                this._xfield = value;
            }
        }

        public bool HasComment => (this._flag & Mcomment) == Mcomment;

        public bool HasExtraField => (this._flag & Mextra) == Mextra;

        public bool HasHeaderCrc => (this._flag & Mhcrc) == Mhcrc;

        public bool HasName => (this._flag & Mname) == Mname;

        public int HeaderSize => (int)this.DataBegin;

        public bool IsCreating => this.OpenMode == FileMode.Create;

        public bool IsValid { get; private set; }

        public DateTime MTime { get; private set; }

        /// <summary>
        ///     Gets or sets the name.
        /// </summary>
        /// <description></description>
        /// The name and comment must be specified before the write of the content, otherwise
        /// They will be left empty!
        /// <value>
        ///     The name.
        /// </value>
        public string Name
        {
            get
            {
                return this.HasName ? this.BytesToString(this._name) : string.Empty;
            }

            set
            {
                if (!this.IsHeaderWritten && !string.IsNullOrEmpty(value))
                {
                    this._name = this.StringToBytes(value);
                    this._flag |= Mname;
                }
            }
        }

        public long OriginalSize => this._origSize;

        public GzOs Os { get; private set; }

        public int PrologLength
            =>
                10 + (this.HasExtraField ? this.ExtraField.TotalLength : 0) + (this.HasName ? this._name.Length : 0)
                + (this.HasComment ? this._comment.Length : 0) + (this.HasHeaderCrc ? 4 : 0);

        public string Ratio
            =>
                this.FileStream.Length <= 0xffffffff
                    ? $"{(double)(this._origSize - this.CompressedDataSize) / this._origSize:P}"
                    : "UNKNOWN";

        private bool IsHeaderWritten { get; set; }

        public virtual void Compress(Stream s)
        {
            this.EnsureHeaderWritten();
            var buf = new byte[Bufsize];
            for (var chunkSize = 1; chunkSize > 0;)
            {
                int bytesRead;
                for (bytesRead = 0; bytesRead < Bufsize && chunkSize > 0;)
                {
                    bytesRead += chunkSize = s.Read(buf, 0, Bufsize);
                }

                this.WriteTo(buf, 0, bytesRead);
            }
        }

        public virtual void CopyTo(Stream s)
        {
            this.EnsureReadMode();
            var buf = new byte[Bufsize];
            for (var bytesRead = Bufsize; bytesRead == Bufsize;)
            {
                bytesRead = this.ReadTo(buf, 0, Bufsize);
                s.Write(buf, 0, bytesRead);
            }
        }

        public virtual void Dispose()
        {
            if (this.IsCreating && this.FileStream != null)
            {
                this.EnsureHeaderWritten();
                this.DeflateStream?.Dispose();
                this.WriteFooter();
            }
            else
            {
                this.DeflateStream?.Dispose();
            }

            this.FileStream?.Dispose();
        }

        public int Read(byte[] buf, int offset, int count)
        {
            this.EnsureReadMode();
            return this.ReadTo(buf, offset, count);
        }

        public virtual byte[] ReadAllBytes()
        {
            this.EnsureReadMode();
            if (this.OriginalSize > 100 * 1024 * 1024)
            {
                throw new NotSupportedException("The file is to big to read its entire content at once!");
            }

            if (this.FileStream.Position != this.DataBegin)
            {
                this.FileStream.Position = this.DataBegin;
            }

            var buf = new byte[this.OriginalSize];
            this.ReadTo(buf, 0, (int)this.OriginalSize);
            return buf;
        }

        public void Write(byte[] buf, int offset, int cnt)
        {
            this.EnsureHeaderWritten();
            this.WriteTo(buf, offset, cnt);
        }

        protected virtual void EnsureHeaderWritten()
        {
            this.EnsureWriteMode();
            if (!this.IsHeaderWritten)
            {
                this.WriteHeader();
                this.WriteNameComment();
                this.IsHeaderWritten = true;
            }
        }

        protected void EnsureReadMode()
        {
            if (this.IsCreating)
            {
                throw new InvalidOperationException("This gzip is in creating mode, read operations are not allowed!");
            }
        }

        protected void EnsureWriteMode()
        {
            if (!this.IsCreating)
            {
                throw new InvalidOperationException("This gzip is in reading mode, write operations are not allowed!");
            }
        }

        protected virtual void ReadExtraField()
        {
            this.ExtraField = new T { FileStream = this.FileStream };
            this.ExtraField.Read();
        }

        protected virtual void ReadHeader()
        {
            var fs = this.FileStream;
            fs.Position = 0;
            var id1 = fs.ReadByte();
            var id2 = fs.ReadByte();
            var compressionMethod = fs.ReadByte();

            // magic number
            if (id1 != GzId1 || id2 != GzId2 || compressionMethod != GzCm)
            {
                this.IsValid = false;
                this.Dispose();
            }
            else
            {
                // header : 10 bytes total
                this._flag = (byte)fs.ReadByte();
                this.MTime = this.ReadUnixTime();
                this._xflag = (byte)fs.ReadByte();
                this.Os = (GzOs)fs.ReadByte();

                if (this.HasExtraField)
                {
                    this.ReadExtraField();
                }

                if (this.HasName)
                {
                    this._name = this.ReadString();
                }

                if (this.HasComment)
                {
                    this._comment = this.ReadString();
                }

                if (this.HasHeaderCrc)
                {
                    this._hcrc = fs.ReadByte() | (fs.ReadByte() << 8);
                }

                this.DataBegin = fs.Position;

                // 				if (_dataBegin != PrologLength)
                // 					throw new Exception("?" + _dataBegin.ToString() + " " + PrologLength.ToString());

                // orig size
                fs.Seek(-8, SeekOrigin.End);
                this.Crc = fs.GetIntLittleEndian();
                this._origSize = fs.GetIntLittleEndian();

                fs.Position = this.DataBegin;
            }
        }

        protected virtual int ReadTo(byte[] buf, int offset, int count)
        {
            var bytesRead = 0;
            for (var chuckSize = 1; bytesRead < count && chuckSize > 0;)
            {
                bytesRead += chuckSize = this.DeflateStream.Read(buf, offset + bytesRead, count - bytesRead);
            }

            return bytesRead;
        }

        protected void WriteFooter()
        {
            this.FileStream.Position = 4;
            this.WriteUnixTime();
            this.FileStream.Seek(0, SeekOrigin.End);
            this.FileStream.SetIntLittleEndian(this.Crc);
            this.FileStream.SetIntLittleEndian(this._origSize);
        }

        protected virtual void WriteHeader()
        {
            this.FileStream.Position = 0;
            this.FileStream.WriteByte(GzId1);
            this.FileStream.WriteByte(GzId2);
            this.FileStream.WriteByte(GzCm);

            this.FileStream.WriteByte(this._flag);

            // the time stamp will be added after the compression of content is completed
            this.FileStream.Seek(4, SeekOrigin.Current);
            this.FileStream.WriteByte(4); // TODO: compression method

            byte os = 255;
            switch (Environment.OSVersion.Platform)
            {
                case PlatformID.Win32NT:
                    os = 0;
                    break;
                case PlatformID.Unix:
                    os = 3;
                    break;
                case PlatformID.MacOSX:
                    os = 7;
                    break;
            }

            this.FileStream.WriteByte(os);
        }

        protected virtual void WriteNameComment()
        {
            // 			if (HasExtraField) //not supported
            // 				ExtraField.Write();
            if (this.HasName || this.HasComment)
            {
                if (this.HasName)
                {
                    this.FileStream.Write(this._name, 0, this._name.Length);
                }

                if (this.HasComment)
                {
                    this.FileStream.Write(this._comment, 0, this._comment.Length);
                }

                this.UpdateFlag();
            }
        }

        protected virtual void WriteTo(byte[] buf, int offset, int cnt)
        {
            this.Crc = Crc32.UpdateCrc(buf, offset, cnt, this.Crc);
            this.DeflateStream.Write(buf, offset, cnt);
            this._origSize += (uint)cnt;
        }

        private string BytesToString(byte[] arr)
        {
            return Encoding.UTF8.GetString(arr, 0, arr.Length - 1);
        }

        private byte[] ReadString()
        {
            var buf = new byte[256];
            var cnt = 0;
            for (var b = -1; b != 0; ++cnt)
            {
                if (cnt >= buf.Length)
                {
                    Array.Resize(ref buf, cnt * 2);
                }

                buf[cnt] = (byte)(b = this.FileStream.ReadByte());
            }

            Array.Resize(ref buf, cnt);
            return buf;
        }

        private DateTime ReadUnixTime()
        {
            uint t = 0;
            for (var i = 0; i < 4; ++i)
            {
                t |= (uint)this.FileStream.ReadByte() << i * 8;
            }

            return UnixStartTime.AddSeconds(Convert.ToDouble(t));
        }

        private byte[] StringToBytes(string s)
        {
            var buff = new byte[Encoding.UTF8.GetByteCount(s) + 1];
            Encoding.UTF8.GetBytes(s, 0, s.Length, buff, 0);
            return buff;
        }

        private void UpdateFlag()
        {
            var pos = this.FileStream.Position;
            this.FileStream.Position = 3;
            this.FileStream.WriteByte(this._flag);
            this.FileStream.Position = pos;
        }

        private void WriteUnixTime()
        {
            var t = (uint)DateTime.Now.Subtract(UnixStartTime).TotalSeconds;
            for (var i = 0; i < 4; ++i)
            {
                this.FileStream.WriteByte((byte)(t >> (8 * i)));
            }
        }
    }

    public class GZip : GZipBase<GzExtraField>
    {
        public GZip(string path, FileMode openMode)
            : base(path, openMode)
        {
        }

        public static GZip Create(string path, bool overwriteIfExists = false)
        {
            if (!overwriteIfExists && File.Exists(path))
            {
                throw new IOException("File " + path + " already exists");
            }

            var gz = new GZip(path, FileMode.Create);
            gz.DeflateStream = new DeflateStream(gz.FileStream, CompressionMode.Compress, true);
            return gz;
        }

        public static GZip OpenRead(string path)
        {
            return new GZip(path, FileMode.Open);
        }
    }
}