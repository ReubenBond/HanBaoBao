namespace DictionaryDbBuilder.WordFrequency
{
    using System;
    using System.Data.SQLite;

    using DictionaryDbBuilder.WordFrequency.JiebaAnalysis;
    using DictionaryDbBuilder.WordFrequency.JunDa;
    using DictionaryDbBuilder.WordFrequency.OpenSubtitles;
    using DictionaryDbBuilder.WordFrequency.SIGHAN2;

    public static class WordFrequencyImporter
    {
        public const string InsertQuery = @"INSERT OR REPLACE INTO frequency(term, occurrences)
    SELECT COALESCE(old.term, new.term), COALESCE(old.occurrences, 0) + COALESCE(new.occurrences, 0)
    FROM (SELECT @term as term, @occurrences as occurrences) AS new
    LEFT JOIN (SELECT * FROM frequency WHERE term=@term)
    AS old ON new.term = old.term";

        private const string MergeQuery =
            @"UPDATE OR IGNORE dictionary set frequency = @frequency where simplified = @term or traditional = @term";

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            // Create temporary database
            new SQLiteCommand(@"create table frequency (term text primary key, occurrences integer);", connection)
                .ExecuteNonQuery();

            // Run each importer
            OpenSubtitles2016Importer.UpdateDatabase(connection, transaction);
            SigHanWordSegmentationBakeoff2Importer.UpdateDatabase(connection, transaction);
            JunDaWordFreqImporter.UpdateDatabase(connection, transaction);
            JiebaAnalysisFreqImporter.UpdateDatabase(connection, transaction);

            // Merge temporary database with dictionary database.
            double total =
                (long)
                new SQLiteCommand("select sum(occurrences) from frequency", connection, transaction).ExecuteScalar();
            var reader =
                new SQLiteCommand("select term, occurrences from frequency", connection, transaction).ExecuteReader();
            var op = new SQLiteCommand(MergeQuery, connection, transaction);
            op.Prepare();
            while (reader.Read())
            {
                op.Parameters.AddWithValue("term", reader.GetString(0));
                op.Parameters.AddWithValue("frequency", Math.Log10(reader.GetInt64(1) / total));
                op.ExecuteNonQuery();
            }

            op.Dispose();

            // Delete temporary database
            new SQLiteCommand(@"drop table frequency", connection, transaction).ExecuteNonQuery();
        }
    }
}