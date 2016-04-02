namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System;
    using System.IO;

    public class TxtHeteroIdxTable : HeteroIdxTable
    {
        private readonly FileStream fs;

        public TxtHeteroIdxTable(string filename, Func<HeteroIdxTable, int> entryRuler)
            : base(entryRuler)
        {
            this.fs = File.OpenRead(filename);
        }

        protected override int Position
        {
            set
            {
                this.fs.Position = value;
            }
        }

        public override byte ReadByte()
        {
            return (byte)this.fs.ReadByte();
        }

        public override byte[] ReadBytes(int offset, int cnt)
        {
            this.fs.Position = offset;
            var buf = new byte[cnt];
            for (int bRead = 0, chSize = 1; bRead < cnt && chSize > 0;)
            {
                bRead += chSize = this.fs.Read(buf, bRead, cnt - bRead);
            }

            return buf;
        }
    }
}