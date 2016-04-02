using System.Linq;

namespace DictionaryDbBuilder.CcCedict
{
    using System.Data.SQLite;
    using System.Text;
    using System.Text.RegularExpressions;

    using DictionaryDbBuilder.Utilities;

    public static class CcCedictImporter
    {
        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var lines = typeof(CcCedictImporter).ReadLines("cedict_ts.u8", Encoding.UTF8).ToList();
            var textQuery =
                new Regex(
                    @"^(?<traditional>[^\s]+)\s+(?<simplified>[^\s]+)\s+\[(?<pinyin>.+)\]\s+(\/(CL:(?<classifier>[^/]+)|(?<definition>[^/]+)))*\/\s*$", 
                    RegexOptions.Compiled);

            var insert =
                new SQLiteCommand(
                    "insert into dictionary (simplified, traditional, pinyin, definition, classifier) values(@simplified, @traditional, @pinyin, @definition, @classifier)", 
                    connection, 
                    transaction);
            insert.Prepare();
            var definition = new StringBuilder();
            foreach (var line in lines)
            {
                if (line.StartsWith("#"))
                {
                    continue;
                }

                var matches = textQuery.Match(line).Groups;
                insert.Parameters.AddWithValue("simplified", matches["simplified"].Value);
                insert.Parameters.AddWithValue("traditional", matches["traditional"].Value);
                var pinyin = matches["pinyin"].Value.Replace("u:", "ü").Replace("U:", "Ü");
                insert.Parameters.AddWithValue("pinyin", pinyin);

                // TODO: Consider JSON Array since there are usually multiple
                var definitionCaptures = matches["definition"].Captures;
                definition.Clear();
                for (var i = 0; i < definitionCaptures.Count; ++i)
                {
                    var value = definitionCaptures[i].Value;
                    if (i > 0)
                    {
                        definition.Append(", ").Append(value);
                    }
                    else
                    {
                        definition.AppendSentenceCase(value);
                    }
                }

                insert.Parameters.AddWithValue("definition", definition.ToString());

                // TODO: may be multiple, consider JSON Array
                var classifier = matches["classifier"].Value;
                insert.Parameters.AddWithValue("classifier", string.IsNullOrWhiteSpace(classifier) ? null : classifier);

                insert.ExecuteNonQuery();
            }
        }
    }
}