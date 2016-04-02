namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System;
    using System.IO;
    using System.Text;

    public class TxtDictDb : IDictDb, IDisposable
    {
        private readonly Encoding encoding;

        private readonly FileStream fileStream;

        public TxtDictDb(string fileName, Encoding encoding = null)
        {
            this.encoding = encoding ?? Encoding.UTF8;
            this.fileStream = File.OpenRead(fileName);
        }

        public void Dispose()
        {
            this.fileStream?.Dispose();
        }

        public byte[] GetBytes(long offset, int length)
        {
            this.fileStream.Position = offset;
            var buf = new byte[length];
            for (int bytesRead = 0, chunkSize = 1; chunkSize > 0;)
            {
                bytesRead += chunkSize = this.fileStream.Read(buf, bytesRead, length - bytesRead);
            }

            return buf;
        }

        public string GetEntry(long offset, int length)
        {
            return this.encoding.GetString(this.GetBytes(offset, length));
        }
    }
}