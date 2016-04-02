namespace DictionaryDbBuilder.WordFrequency.SIGHAN2
{
    using System;
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.IO;
    using System.Linq;

    using DictionaryDbBuilder.Utilities;

    public static class SigHanWordSegmentationBakeoff2Importer
    {
        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var dbFileName =
                Path.Combine(
                    new[] { typeof(SigHanWordSegmentationBakeoff2Importer).GetAssemblyPath() }.Concat(
                        typeof(SigHanWordSegmentationBakeoff2Importer).Namespace.Split('.')
                            .Skip(1)
                            .Concat(new[] { "cache", "sighan_bakeoff2.db" })).ToArray());

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
            new SQLiteCommand("create table frequency (term text primary key, occurrences numeric)", connection)
                .ExecuteNonQuery();
            var folder =
                Path.Combine(
                    new[] { typeof(SigHanWordSegmentationBakeoff2Importer).GetAssemblyPath() }.Concat(
                        typeof(SigHanWordSegmentationBakeoff2Importer).Namespace.Split('.').Skip(1)).ToArray());
            long total = 0;
            var words = new Dictionary<string, int>();
            var allWords =
                Directory.EnumerateFiles(Path.Combine(folder, "files"))
                    .SelectMany(file => File.ReadAllLines(file).SelectMany(line => line.Split('　', ' ')))
                    .Where(word => !string.IsNullOrWhiteSpace(word));
            foreach (var word in allWords)
            {
                int frequency;
                if (!words.TryGetValue(word, out frequency))
                {
                    words.Add(word, 0);
                }

                words[word] += 1;
                total++;
            }

            Console.WriteLine($"{words.Count} unique words, {total} total");

            using (var transaction = connection.BeginTransaction())
            {
                var op = new SQLiteCommand(
                    "insert into frequency(term, occurrences) values (@term, @occurrences)", 
                    connection, 
                    transaction);
                op.Prepare();
                foreach (var pair in words)
                {
                    op.Parameters.AddWithValue("term", pair.Key);
                    op.Parameters.AddWithValue("occurrences", pair.Value);

                    op.ExecuteNonQuery();
                }

                op.Dispose();
                transaction.Commit();
            }
        }
    }
}