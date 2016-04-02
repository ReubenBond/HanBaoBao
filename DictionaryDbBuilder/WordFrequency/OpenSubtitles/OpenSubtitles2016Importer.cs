namespace DictionaryDbBuilder.WordFrequency.OpenSubtitles
{
    using System;
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.Xml.XPath;

    using DictionaryDbBuilder.Utilities;
    using DictionaryDbBuilder.Utilities.GZip;

    using Microsoft.IO;

    public static class OpenSubtitles2016Importer
    {
        // The open subtitles "zh" corpus from http://opus.lingfil.uu.se/OpenSubtitles2016.php
        private const string CorpusPath = @"C:\Users\reube\Downloads\zh.tar\OpenSubtitles2016\xml\zh\";

        private static readonly RecyclableMemoryStreamManager MemoryPool = new RecyclableMemoryStreamManager();

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var dbFileName =
                Path.Combine(
                    new[] { typeof(OpenSubtitles2016Importer).GetAssemblyPath() }.Concat(
                        typeof(OpenSubtitles2016Importer).Namespace.Split('.')
                            .Skip(1)
                            .Concat(new[] { "files", "opensubs.db" })).ToArray());

            if (!File.Exists(dbFileName))
            {
                ProcessCorpus(dbFileName);
            }

            var corpusDbConnection = new SQLiteConnection($"Data Source={dbFileName};Version=3");
            corpusDbConnection.Open();
            var reader =
                new SQLiteCommand("select term, occurrences from frequency", corpusDbConnection).ExecuteReader();

            var op = new SQLiteCommand(WordFrequencyImporter.InsertQuery, connection, transaction);
            op.Prepare();
            while (reader.Read())
            {
                op.Parameters.AddWithValue("term", reader.GetString(0));
                op.Parameters.AddWithValue("occurrences", reader.GetInt64(1));
                op.ExecuteNonQuery();
            }

            op.Dispose();
        }

        private static void ProcessCorpus(string dbFileName)
        {
            SQLiteConnection.CreateFile(dbFileName);
            var connection = new SQLiteConnection($"Data Source={dbFileName};Version=3");
            connection.Open();
            new SQLiteCommand("create table frequency (term text primary key, occurrences integer)", connection)
                .ExecuteNonQuery();

            long total = 0;
            long totalBytes = 0;
            var words = new Dictionary<string, int>();
            var allGZippedFiles =
                Directory.EnumerateDirectories(CorpusPath)
                    .SelectMany(Directory.EnumerateDirectories)
                    .SelectMany(Directory.EnumerateFiles);
            foreach (var gzFile in allGZippedFiles)
            {
                if (!gzFile.EndsWith(".gz"))
                {
                    continue;
                }

                using (var gz = GZip.OpenRead(gzFile))
                using (var fs = gz.DeflateStream)
                using (var ms = MemoryPool.GetStream())
                {
                    ms.SetLength(0);
                    fs.CopyTo(ms);
                    ms.Position = 0;
                    totalBytes += ms.Length;
                    try
                    {
                        var doc = new XPathDocument(ms);
                        var xml = doc.CreateNavigator();
                        foreach (var node in xml.Select("/document/s/w").OfType<XPathNavigator>())
                        {
                            if (string.IsNullOrWhiteSpace(node.InnerXml))
                            {
                                continue;
                            }

                            int frequency;
                            if (!words.TryGetValue(node.InnerXml, out frequency))
                            {
                                words.Add(node.InnerXml, 0);
                            }

                            words[node.InnerXml] += 1;
                            total++;
                        }
                    }
                    catch (Exception exception)
                    {Debug.WriteLine($"Exception processing document {gzFile}: " + exception);
                    }
                }
            }

            Console.WriteLine(
                $"{words.Count} unique words, {total} total occurrences, processed {totalBytes / 1000000}MB of raw input");

            using (var transaction = connection.BeginTransaction())
            {
                var op = new SQLiteCommand(
                    "insert into frequency(term, occurrences) values (@term, @occurrences)", 
                    connection);
                op.Prepare();
                foreach (var pair in words)
                {
                    op.Parameters.AddWithValue("term", pair.Key);
                    op.Parameters.AddWithValue("occurrences", pair.Value);

                    op.ExecuteNonQuery();
                }

                transaction.Commit();
            }
        }
    }
}