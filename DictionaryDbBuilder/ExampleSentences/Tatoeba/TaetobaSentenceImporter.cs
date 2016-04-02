namespace DictionaryDbBuilder.ExampleSentences.Tatoeba
{
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.IO;
    using System.Linq;

    using CsvHelper;

    using DictionaryDbBuilder.Utilities;

    public static class TatoebaSentenceImporter
    {
        public static string GetOrDefault(string[] collection, int index)
        {
            if (collection.Length > index && !string.IsNullOrWhiteSpace(collection[index]) && collection[index] != "\\N")
            {
                return collection[index];
            }

            return null;
        }

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var folder =
                Path.Combine(
                    new[] { typeof(TatoebaSentenceImporter).GetAssemblyPath() }.Concat(
                        typeof(TatoebaSentenceImporter).Namespace.Split('.').Skip(1)).ToArray());

            var translations = new Dictionary<int, int>(500000);
            using (var file = File.OpenText(Path.Combine(folder, "links.csv")))
            using (var csv = new CsvReader(file) { Configuration = { Delimiter = "\t" } })
            {
                /* var insert = new SQLiteCommand("insert into sentence_links (original, translated) values (@original, @translated)", connection, transaction);
                     insert.Prepare();*/
                while (csv.Read())
                {
                    var orig = int.Parse(csv[0]);
                    var trans = int.Parse(csv[1]);
                    translations[orig] = trans;

                    /*insert.Parameters.AddWithValue("original", orig);
                        insert.Parameters.AddWithValue("translated", trans);
                        insert.ExecuteNonQuery();*/
                }
            }

            using (var file = File.OpenText(Path.Combine(folder, "sentences.csv")))
            using (var csv = new CsvReader(file) { Configuration = { Delimiter = "\t" } })
            using (
                var insert =
                    new SQLiteCommand(
                        "insert into sentence (rowid, text, lang, trans_of) values (@id, @text, @lang, @trans_of)",
                        connection,
                        transaction))
            {
                insert.Prepare();
                var sentence = new Sentence();
                while (csv.Read())
                {
                    if (csv[1] != "eng" && csv[1] != "cmn")
                    {
                        continue;
                    }

                    sentence.Id = int.Parse(csv[0]);

                    int translationOf;
                    if (!translations.TryGetValue(sentence.Id, out translationOf))
                    {
                        continue;
                    }

                    sentence.TranslationOf = translationOf;
                    sentence.Language = csv[1];
                    sentence.Text = csv[2];

                    sentence.AddToParameters(insert.Parameters);
                    insert.ExecuteNonQuery();
                }
            }

            // Delete sentences.
            /*new SQLiteCommand(
                    "DELETE FROM sentence_links WHERE rowid IN (SELECT sentence_links.rowid FROM sentence_links LEFT JOIN sentence ON sentence.rowid=sentence_links.rowid WHERE sentence.rowid IS NULL);",
                    connection).ExecuteNonQuery();*/
        }

        private class Sentence
        {
            public int Id { get; set; }

            public string Language { get; set; }

            public string Text { get; set; }

            public int TranslationOf { get; set; }

            public void AddToParameters(SQLiteParameterCollection parameters)
            {
                parameters.Clear();
                parameters.AddWithValue("id", this.Id);
                parameters.AddWithValue("text", this.Text);
                parameters.AddWithValue("lang", this.Language);
                parameters.AddWithValue("trans_of", this.TranslationOf);
            }
        }
    }
}