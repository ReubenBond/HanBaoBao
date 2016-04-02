namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System;

    public abstract class HeteroIdxTable
    {
        private readonly Func<HeteroIdxTable, int> entryRuler;

        protected HeteroIdxTable(Func<HeteroIdxTable, int> entryRuler)
        {
            this.entryRuler = entryRuler;
        }

        protected abstract int Position { set; }

        public int FindNext(int offset)
        {
            this.Position = offset;
            return offset + this.entryRuler(this);
        }

        public byte[] GetEntry(int address)
        {
            this.Position = address;
            var cnt = this.entryRuler(this);
            return this.ReadBytes(address, cnt);
        }

        public abstract byte ReadByte();

        public abstract byte[] ReadBytes(int offset, int cnt);
    }
}