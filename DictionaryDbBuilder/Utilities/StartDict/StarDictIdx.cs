namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System;
    using System.IO;
    using System.Text;

    public class StarDictIdx : IDictIdx
    {
        private readonly Encoding encoding;

        private readonly int[] indexIndex; // index of index

        private readonly int offsetSize; // offset word size

        private readonly HeteroIdxTable table;

        public StarDictIdx(StarDictInfo info, Encoding enc = null)
        {
            this.encoding = enc ?? Encoding.UTF8;
            this.offsetSize = info.PointerSize;

            var fname = info.BaseName + ".idx";
            if (File.Exists(fname))
            {
                this.table = new TxtHeteroIdxTable(fname, this.MeasureEntryLength);
            }
            else
            {
                this.table = new StrHeteroIdxTable(fname + ".gz", this.MeasureEntryLength);
            }

            // create and populate the index of index
            this.indexIndex = new int[info.NumberOfEntries + 1];
            for (int cnt = 1, offset = 0; cnt < info.NumberOfEntries; ++cnt)
            {
                this.indexIndex[cnt] = offset = this.table.FindNext(offset);
            }

            this.indexIndex[info.NumberOfEntries] = info.IndexFileSize; // last sentry
        }

        private int AddressSize => this.offsetSize + 4;

        public bool GetAddress(int ordinal, out long offset, out int length)
        {
            if (ordinal < 0 || ordinal >= this.indexIndex.Length - 1)
            {
                offset = length = 0;
                return false;
            }

            var bs = this.table.ReadBytes(this.indexIndex[ordinal + 1] - this.AddressSize, this.AddressSize);
            offset = (long)bs.GetLongBigEndian(0, this.offsetSize);
            length = (int)bs.GetIntBigEndian(this.offsetSize);
            return true;
        }

        public bool GetIndexRange(string headword, out int begin, out int end)
        {
            begin = 0;
            end = this.indexIndex.Length - 1;
            while (begin < end)
            {
                var mid = (begin + end) / 2;
                if (this.CompareHw(this.GetHeadword(mid), headword) < 0)
                {
                    begin = mid + 1;
                }
                else
                {
                    end = mid;
                }
            }

            end = begin;
            var hi = this.indexIndex.Length - 1;
            while (end < hi)
            {
                var mid = (end + hi) / 2;
                if (this.CompareHw(this.GetHeadword(mid), headword) <= 0)
                {
                    end = mid + 1;
                }
                else
                {
                    hi = mid;
                }
            }

            return begin != end;
        }

        private int CompareHw(string sa, string sb)
        {
            var rel = string.Compare(sa, sb, StringComparison.OrdinalIgnoreCase);
            return rel == 0 ? string.CompareOrdinal(sa, sb) : rel;
        }

        private string GetHeadword(int idx)
        {
            int start = this.indexIndex[idx], end = this.indexIndex[idx + 1];
            var arr = this.table.ReadBytes(start, end - start - this.AddressSize - 1);
            return this.encoding.GetString(arr);
        }

        private int MeasureEntryLength(HeteroIdxTable tbl)
        {
            for (var cnt = 0;; ++cnt)
            {
                if (tbl.ReadByte() == 0)
                {
                    return cnt + 1 + this.AddressSize; // '\0' + sizeof(offsetPtr) + 4 bytes size
                }
            }
        }
    }
}