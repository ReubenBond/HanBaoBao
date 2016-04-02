namespace DictionaryDbBuilder.Adso
{
    using System.Data.SQLite;
    using System.IO;
    using System.Linq;
    using System.Text;

    using Common.Models;

    using DictionaryDbBuilder.Utilities;

    public static class AdsoTransImporter
    {
        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var path =
                Path.Combine(
                    new[] { typeof(AdsoTransImporter).GetAssemblyPath() }.Concat(
                        typeof(AdsoTransImporter).Namespace.Split('.').Skip(1).Concat(new[] { "files", "adso.db" }))
                        .ToArray());

            var adsoConnection = new SQLiteConnection($"Data Source={path};Version=3");
            adsoConnection.Open();
            var reader =
                new SQLiteCommand(
                    "select chinese_utf8_s, chinese_utf8_c, pinyin2, flag, english from expanded_unified",
                    adsoConnection).ExecuteReader();

            const string Sql = @"INSERT OR REPLACE INTO dictionary
    (
        rowid,
        simplified,
        traditional,
        pinyin,
        definition,
        classifier,
        hsk_level,
        part_of_speech,
        frequency,
        concept,
        topic,
        parent_topic,
        notes
    )
    SELECT
        old.rowid,
        COALESCE(old.simplified, new.simplified),
        COALESCE(old.traditional, new.traditional),
        COALESCE(old.pinyin, new.pinyin),
        COALESCE(old.definition, new.definition),
        old.classifier,
        old.hsk_level,
        COALESCE(old.part_of_speech, 0) | new.part_of_speech,
        old.frequency,
        old.concept,
        old.topic,
        old.parent_topic,
        old.notes
    FROM (SELECT @simplified AS simplified, @traditional AS traditional, @pinyin AS pinyin, @definition AS definition, @part_of_speech as part_of_speech) AS new
    LEFT JOIN (SELECT * FROM dictionary WHERE simplified=@simplified and simplified not null)
    AS old ON new.simplified = old.simplified or new.traditional = old.traditional";
            using (var insert = new SQLiteCommand(Sql, connection, transaction))
            {
                insert.Prepare();

                var entry = new Entry();
                var pinyinStringBuilder = new StringBuilder();
                while (reader.Read())
                {
                    entry.Clear();
                    entry.Simplified = reader.GetString(0);
                    entry.Traditional = reader.GetString(1);
                    entry.Pinyin = PinyinUtil.NormalizeNumbered(reader.GetString(2), pinyinStringBuilder);
                    entry.AddPartOfSpeech(reader.GetString(3));
                    entry.Definition = reader.GetString(4);

                    var p = insert.Parameters;
                    p.AddWithValue("simplified", entry.Simplified);
                    p.AddWithValue("traditional", entry.Traditional);
                    p.AddWithValue("pinyin", entry.Pinyin);

                    // The ADSO dictionary is overzealous about nouns, likely because it is designed primarily for machine translation, so we ignore nouns from there.
                    p.AddWithValue("part_of_speech", entry.PartOfSpeech & ~PartOfSpeech.Noun);
                    p.AddWithValue("definition", entry.Definition.ToSentenceCase());
                    insert.ExecuteNonQuery();
                }
            }

            adsoConnection.Close();
            }
        }
    }
