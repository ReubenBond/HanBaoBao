namespace DictionaryDbBuilder.ExampleSentences
{
    using System.Data.SQLite;

    public static class ExampleSentenceImporter
    {
        public static string GetOrDefault(string[] collection, int index)
        {
            if (collection.Length > index && !string.IsNullOrWhiteSpace(collection[index]) && collection[index] != "\\N")
            {
                return collection[index];
            }

            return null;
        }

        public static int UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            using (var cmd = new SQLiteCommand(@"
CREATE TABLE sentence (
    rowid   INTEGER PRIMARY KEY,
	text	TEXT,
	lang	TEXT,
	trans_of INTEGER
);

CREATE INDEX sentence_trans_of on sentence(trans_of);", connection, transaction))
            {
                cmd.ExecuteNonQuery();
            }

            return 0;
        }
    }
}