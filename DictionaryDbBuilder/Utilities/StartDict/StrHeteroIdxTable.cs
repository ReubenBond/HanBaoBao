namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System;

    using DictionaryDbBuilder.Utilities.GZip;

    public class StrHeteroIdxTable : HeteroIdxTable
    {
        private readonly byte[] tbl;

        private int startPos;

        public StrHeteroIdxTable(byte[] tbl, Func<HeteroIdxTable, int> entryRuler)
            : base(entryRuler)
        {
            this.tbl = tbl;
        }

        public StrHeteroIdxTable(string gzName, Func<HeteroIdxTable, int> entryRuler)
            : this(GZip.OpenRead(gzName).ReadAllBytes(), entryRuler)
        {
        }

        protected override int Position
        {
            set
            {
                this.startPos = value;
            }
        }

        public override byte ReadByte()
        {
            return this.tbl[this.startPos++];
        }

        public override byte[] ReadBytes(int offset, int cnt)
        {
            var buf = new byte[cnt];
            Buffer.BlockCopy(this.tbl, offset, buf, 0, cnt);
            return buf;
        }
    }
}