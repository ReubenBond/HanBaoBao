namespace DictionaryDbBuilder.Utilities
{
    using System;
    using System.Collections.Generic;
    using System.IO;
    using System.Text;

    public static class Resource
    {
        public static Stream GetEmbeddedFile(this Type type, string fileName)
        {
            return type.Assembly.GetManifestResourceStream(type, fileName);
        }

        public static string GetEmbeddedFileContents(this Type type, string fileName)
        {
            using (var stream = type.GetEmbeddedFile(fileName))
            using (var reader = new StreamReader(stream, Encoding.UTF8))
            {
                return reader.ReadToEnd();
            }
        }

        public static IEnumerable<string> ReadLines(this Type type, string fileName, Encoding encoding)
        {
            using (var stream = type.GetEmbeddedFile(fileName))
            using (var reader = new StreamReader(stream, encoding))
            {
                string line;
                while ((line = reader.ReadLine()) != null)
                {
                    yield return line;
                }
            }
        }
    }
}