namespace DictionaryDbBuilder.WordFrequency.JiebaAnalysis
{
    using System.Data.SQLite;
    using System.Text;

    using DictionaryDbBuilder.Utilities;

    /// <summary>
    ///     Imports data from https://github.com/huaban/jieba-analysis
    /// </summary>
    public static class JiebaAnalysisFreqImporter
    {
        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var op = new SQLiteCommand(WordFrequencyImporter.InsertQuery, connection, transaction);
            op.Prepare();
            foreach (var line in typeof(JiebaAnalysisFreqImporter).ReadLines("dict.txt", Encoding.UTF8))
            {
                if (string.IsNullOrWhiteSpace(line))
                {
                    continue;
                }

                var tokens = line.Split(' ');
                if (tokens.Length < 2)
                {
                    continue;
                }

                var word = tokens[0];
                var freq = int.Parse(tokens[1]);
                op.Parameters.AddWithValue("term", word);
                op.Parameters.AddWithValue("occurrences", freq);
                op.ExecuteNonQuery();
            }
        }
    }
}