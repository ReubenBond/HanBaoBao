namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System.IO;
    using System.Text;

    using DictionaryDbBuilder.Utilities.GZip;

    public class StarDict
    {
        private readonly IDictDb database;

        private readonly Encoding encoding;

        private readonly IDictIdx index;

        private StarDictInfo info;

        private StarDict(StarDictInfo info, IDictIdx index, string fileName, Encoding encoding)
        {
            this.encoding = encoding ?? Encoding.UTF8;
            this.info = info;
            this.index = index;
            this.database = fileName.EndsWith("dz") ? DictZip.OpenRead(fileName) : (IDictDb)new TxtDictDb(fileName);
        }

        public static StarDict TryOpen(string baseFileName, Encoding encoding = null)
        {
            var info = new StarDictInfo(baseFileName + ".ifo");
            if (!info.IsValid)
            {
                return null;
            }

            var idx = new StarDictIdx(info);
            return new StarDict(
                info, 
                idx, 
                baseFileName + (File.Exists(baseFileName + ".dict") ? ".dict" : ".dict.dz"), 
                encoding);
        }

        public string[] Search(string headword)
        {
            int begin, end;
            this.index.GetIndexRange(headword, out begin, out end);
            var res = new string[end - begin];
            for (int cnt = 0, i = begin; i < end; ++i)
            {
                long offset;
                int len;
                this.index.GetAddress(i, out offset, out len);
                res[cnt++] = this.encoding.GetString(this.database.GetBytes(offset, len));
            }

            return res;

            // TODO: synonym file support
        }
    }
}