namespace DictionaryDbBuilder.Utilities.GZip
{
    using System;
    using System.IO;
    using System.IO.Compression;
    using System.Text;

    using DictionaryDbBuilder.Utilities.StartDict;

    public class DzExtraField : GzExtraField
    {
        public static readonly ushort Defaultchunklen = 58315;

        private ushort _chLen = Defaultchunklen;

        public DzExtraField()
        {
            this.Id[0] = 82;
            this.Id[1] = 65;
        }

        public ushort ChunkCount { get; private set; }

        public ushort ChunkSize
        {
            get
            {
                return this._chLen;
            }

            // may only set once
            internal set
            {
                if (value < 1024 || value > Defaultchunklen)
                {
                    throw new ArgumentOutOfRangeException(
                        "The chunk size must be less than 64KB and yet it shouldn't be"
                        + " too small or the compression ratio would degrade dramatically");
                }

                this._chLen = value;
            }
        }

        public int IndexTableBegin => this.FieldDataBegin + 6;

        public uint[] Indices { get; private set; }

        public ushort Version { get; private set; }

        internal void Finish()
        {
            this.Length = (int)(this.Fs.Length - this.FieldDataBegin);
            this.Fs.Position = 10;
            this.Fs.SetShortLittleEndian((ushort)this.XLength);
            this.Fs.Position += 2;
            this.Fs.SetShortLittleEndian((ushort)this.Length);
            this.Fs.Position = this.FieldDataBegin + 4;

            // chunk count
            this.Fs.SetShortLittleEndian((ushort)((this.Length - 6) / 2));
        }

        internal override void Read()
        {
            base.Read();
            this.Fs.Position = this.FieldDataBegin;
            this.Version = (ushort)this.Fs.GetIntLittleEndian(2);
            this.ChunkSize = (ushort)this.Fs.GetIntLittleEndian(2);
            this.ChunkCount = (ushort)this.Fs.GetIntLittleEndian(2);

            this.Indices = new uint[this.ChunkCount + 1];
            this.Indices[0] = 0;

            // read index
            var idxTableSize = this.ChunkCount * (this.Version == 1 ? 2 : 4); // seems only version 1 exists

            var buf = new byte[idxTableSize];

            for (int bytesRead = 0, chSize = 1; bytesRead < idxTableSize && chSize > 0;)
            {
                bytesRead += chSize = this.Fs.Read(buf, bytesRead, idxTableSize - bytesRead);
            }

            // convert individual chunk sizes to offsets
            for (uint sum = 0, i = 0; i < this.ChunkCount; ++i)
            {
                sum += (uint)(buf[i * 2] | (buf[i * 2 + 1] << 8));
                this.Indices[i + 1] = sum;
            }
        }

        internal void UpdateChunkCount()
        {
            this.FileStream.Position = this.FieldDataBegin + 4;
            this.FileStream.SetIntLittleEndian((uint)(this.FileStream.Length - this.IndexTableBegin), 2);
            this.FileStream.Seek(0, SeekOrigin.End);
        }

        internal void WritePreliminary()
        {
            this.Fs.Seek(12, SeekOrigin.Begin);
            this.Fs.WriteByte((byte)'R');
            this.Fs.WriteByte((byte)'A');

            // LEN skipped
            this.Fs.Position = this.FieldDataBegin;
            this.Fs.SetShortLittleEndian(this.Version = 1);
            this.Fs.SetShortLittleEndian(0); // chunk count will be set after the compression is done
        }
    }

    // ugly ! 
    public class DictZip : GZipBase<DzExtraField>, IDictDb
    {
        private const int Bufsize = 1024 * 64;

        private readonly Encoding _enc;

        private readonly FileStream _temp;

        private readonly string _tempName;

        private byte[] _buf;

        private int _chOffset;

        private long _lastChunkEnd;

        private int _lastEnd = -1;

        private int _lastStart = -1;

        private DictZip(string path, int chunkSize, FileMode mode, Encoding enc = null)
            : base(path, mode)
        {
            this._enc = enc ?? Encoding.UTF8;

            // Because the index table of chunks keeps growing as the compressed data grows
            // they're better handled separately. At the disposing phase these two parts
            // will be concatenated to produce the final target archive.
            if (this.IsCreating)
            {
                this._temp = File.Create(this._tempName = Path.GetTempFileName());
                this._lastChunkEnd = 0;
                this.ExtraField = new DzExtraField { IsCreating = true, FileStream = this.FileStream };
                if (chunkSize != 0)
                {
                    this.ExtraField.ChunkSize = (ushort)chunkSize;
                }

                this.DeflateStream = new DeflateStream(this._temp, CompressionMode.Compress, true);
                this.WriteHeader();
                this.ExtraField.WritePreliminary();
            }
        }

        private int SpaceToFill => this.ExtraField.ChunkSize - this._chOffset;

        public static DictZip Create(
            string path, 
            bool overwriteIfExists = false, 
            Encoding enc = null, 
            int chunkSize = 0)
        {
            if (!overwriteIfExists && File.Exists(path))
            {
                throw new IOException("File " + path + " already exists!");
            }

            return new DictZip(path, chunkSize, FileMode.Create, enc);
        }

        public static DictZip OpenRead(string path, Encoding enc = null)
        {
            return new DictZip(path, 0, FileMode.Open, enc);
        }

        public override void Dispose()
        {
            if (!this.IsCreating)
            {
                base.Dispose();
            }
            else
            {
                if (this._chOffset > 0)
                {
                    this.ChunkDone();
                }

                this.DeflateStream.Dispose();
                this._temp.Dispose();
                this.ExtraField.Finish();
                this.FileStream.Seek(0, SeekOrigin.End);
                this.WriteNameComment();
                using (var temp = File.OpenRead(this._tempName))
                {
                    temp.CopyTo(this.FileStream);
                }

                this.WriteFooter();
                this.FileStream.Dispose();
                File.Delete(this._tempName);
            }
        }

        public byte[] GetBytes(long pos, int cnt)
        {
            return this.ReadAt((int)pos, cnt);
        }

        public string GetEntry(long pos, int cnt)
        {
            return this._enc.GetString(this.ReadAt((int)pos, cnt));
        }

        public override byte[] ReadAllBytes()
        {
            if (this.OriginalSize > 100 * 1024 * 1024)
            {
                throw new NotSupportedException("The file is to big to read its entire content at once!");
            }

            return this.ReadAt(0, (int)this.OriginalSize);
        }

        public byte[] ReadAt(int pos, int cnt)
        {
            this.EnsureReadMode();
            var end = pos + cnt - 1;
            if (pos < 0 || end > this.OriginalSize)
            {
                throw new ArgumentOutOfRangeException();
            }

            var startIdx = pos / this.ExtraField.ChunkSize;
            var offset = pos % this.ExtraField.ChunkSize;
            var endIdx = end / this.ExtraField.ChunkSize;

            if (startIdx != this._lastStart || endIdx != this._lastEnd || this._buf == null)
            {
                this._lastStart = startIdx;
                this._lastEnd = endIdx;
                this._buf = new byte[(endIdx - startIdx + 1) * this.ExtraField.ChunkSize];

                // read chunks
                for (var i = startIdx; i <= endIdx; ++i)
                {
                    this.FileStream.Position = this.DataBegin + this.ExtraField.Indices[i];
                    this.DeflateStream.Dispose();
                    this.DeflateStream = new DeflateStream(this.FileStream, CompressionMode.Decompress, true);

                    // If I keep using the same deflatestream the data will become corrupted 
                    // when crossing chunks, I don't no why :(
                    this.Read(this._buf, (i - startIdx) * this.ExtraField.ChunkSize, this.ExtraField.ChunkSize);
                }
            }

            var res = new byte[cnt];
            Buffer.BlockCopy(this._buf, offset, res, 0, cnt);
            return res;
        }

        protected override void EnsureHeaderWritten()
        {
            this.EnsureWriteMode();
        }

        protected override void WriteTo(byte[] buf, int offset, int cnt)
        {
            while (cnt > 0)
            {
                var size = cnt > this.SpaceToFill ? this.SpaceToFill : cnt;
                base.WriteTo(buf, offset, size);

                if (size == this.SpaceToFill)
                {
                    this.ChunkDone();
                }
                else
                {
                    this._chOffset += size;
                }

                cnt -= size;
                offset += size;
            }
        }

        private void ChunkDone()
        {
            // the deflate stream must be flushed first so that the size of underlying file can 
            // be updated accordingly
            this.DeflateStream.Dispose();

            // add a idx entry
            this.FileStream.SetShortLittleEndian((ushort)(this._temp.Length - this._lastChunkEnd));
            this._lastChunkEnd = this._temp.Length;
            this.DeflateStream = new DeflateStream(this._temp, CompressionMode.Compress, true);
            this._chOffset = 0;
        }
    }
}