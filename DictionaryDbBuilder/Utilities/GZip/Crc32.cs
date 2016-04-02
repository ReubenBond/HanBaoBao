namespace DictionaryDbBuilder.Utilities.GZip
{
    using System;
    using System.IO;

    public class Crc32
    {
        public const uint Polynomial = 0xedb88320;

        public const uint Seed = 0xffffffff;

        private const int Arrlen = 4 * 1024;

        private static readonly uint[] Table = new uint[256];

        private static readonly byte[] Buf = new byte[Arrlen];

        static Crc32()
        {
            for (uint i = 0; i < Table.Length; ++i)
            {
                var c = i;
                for (var j = 8; j > 0; --j)
                {
                    if ((c & 1) == 1)
                    {
                        c = (c >> 1) ^ Polynomial;
                    }
                    else
                    {
                        c >>= 1;
                    }
                }

                Table[i] = c;
            }
        }

        public static uint Compute(Stream s)
        {
            Array.Clear(Buf, 0, Arrlen);
            uint crc = 0;
            for (int bytesRead = 0, chunkSize = 1; chunkSize > 0; bytesRead = 0)
            {
                while (bytesRead < Arrlen && chunkSize > 0)
                {
                    bytesRead += chunkSize = s.Read(Buf, 0, Arrlen);
                }

                crc = UpdateCrc(Buf, 0, bytesRead, crc);
            }

            return crc;
        }

        public static uint Compute(byte[] arr)
        {
            return UpdateCrc(arr, 0, arr.Length);
        }

        public static uint UpdateCrc(byte[] buf, int offset, int count, uint crc = 0)
        {
            crc ^= Seed;
            for (var i = offset; i < offset + count; ++i)
            {
                crc = Table[(crc ^ buf[i]) & 0xff] ^ (crc >> 8);
            }

            return ~crc;
        }
    }
}