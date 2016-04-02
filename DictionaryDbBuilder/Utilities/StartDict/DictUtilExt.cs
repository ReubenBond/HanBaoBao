namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System.IO;

    public static class DictUtilExt
    {
        public static uint GetIntBigEndian(this Stream s, int size = 4)
        {
            uint res = 0;
            for (; size-- > 0; res |= (uint)s.ReadByte() << (size * 8))
            {
            }

            return res;
        }

        public static uint GetIntBigEndian(this byte[] arr, int idx = 0, int size = 4)
        {
            uint res = 0;
            for (; size-- > 0; res |= (uint)arr[idx++] << (size * 8))
            {
            }

            return res;
        }

        public static uint GetIntLittleEndian(this Stream s, int size = 4)
        {
            uint res = 0;
            for (var i = 0; i < size; ++i)
            {
                res |= (uint)s.ReadByte() << (i * 8);
            }

            return res;
        }

        public static uint GetIntLittleEndian(this byte[] arr, int idx = 0, int size = 4)
        {
            uint res = 0;
            for (var i = 0; i < size; res |= (uint)arr[idx++] << (i++ * 8))
            {
            }

            return res;
        }

        public static ulong GetLongBigEndian(this byte[] arr, int idx = 0, int size = 8)
        {
            ulong res = 0;
            for (; size-- > 0; res |= (ulong)arr[idx++] << (size * 8))
            {
            }

            return res;
        }

        public static ulong GetLongLittleEndian(this byte[] arr, int idx = 0, int size = 8)
        {
            ulong res = 0;
            for (var i = 0; i < size; res |= (ulong)arr[idx++] << (i++ * 8))
            {
            }

            return res;
        }

        public static ulong GetUlongBigEndian(this Stream s, int size = 8)
        {
            ulong res = 0;
            for (; size-- > 0; res |= (ulong)s.ReadByte() << (size * 8))
            {
            }

            return res;
        }

        public static void SetIntLittleEndian(this Stream s, uint num, int size = 4)
        {
            for (var i = 0; i < 4; ++i)
            {
                s.WriteByte((byte)(num >> (i * 8)));
            }
        }

        public static void SetShortLittleEndian(this Stream s, ushort num)
        {
            s.WriteByte((byte)num);
            s.WriteByte((byte)(num >> 8));
        }
    }
}