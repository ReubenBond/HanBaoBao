namespace DictionaryDbBuilder.WordFrequency.JunDa
{
    using System.Data.SQLite;
    using System.Linq;
    using System.Text;

    using DictionaryDbBuilder.Utilities;

    public static class JunDaWordFreqImporter
    {
        private static readonly string[] FileNames = { "CharFreq-Modern.txt" };

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var op = new SQLiteCommand(WordFrequencyImporter.InsertQuery, connection, transaction);
            op.Prepare();
            foreach (
                var line in
                    FileNames.SelectMany(
                        fileName => typeof(JunDaWordFreqImporter).ReadLines(fileName, Encoding.Unicode)))
            {
                if (string.IsNullOrWhiteSpace(line))
                {
                    continue;
                }

                if (!char.IsDigit(line[0]))
                {
                    continue;
                }

                var splits = line.Split('\t');
                if (splits.Length < 5)
                {
                    continue;
                }

                var word = splits[1];
                var frequency = int.Parse(splits[2]);
                op.Parameters.AddWithValue("term", word);
                op.Parameters.AddWithValue("occurrences", frequency);
                op.ExecuteNonQuery();
            }

            op.Dispose();
        }
    }
}