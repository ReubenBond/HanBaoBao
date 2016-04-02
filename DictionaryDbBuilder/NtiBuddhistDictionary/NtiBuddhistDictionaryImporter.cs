namespace DictionaryDbBuilder.NtiBuddhistDictionary
{
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Text;

    using DictionaryDbBuilder.Utilities;

    public static class NtiBuddhistDictionaryImporter
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
            var lines = typeof(NtiBuddhistDictionaryImporter).ReadLines("words.txt", Encoding.UTF8);
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
        COALESCE(old.concept, new.concept),
        COALESCE(old.topic, new.topic),
        COALESCE(old.parent_topic, new.parent_topic),
        COALESCE(old.notes, new.notes)
    FROM (SELECT @simplified AS simplified, @traditional AS traditional, @pinyin AS pinyin, @definition AS definition, @part_of_speech as part_of_speech, @concept as concept, @topic as topic, @parent_topic as parent_topic, @notes as notes) AS new
    LEFT JOIN (SELECT * FROM dictionary WHERE simplified=@simplified and simplified not null)
    AS old ON new.simplified = old.simplified or new.traditional = old.traditional";

            var insert = new SQLiteCommand(Sql, connection, transaction);
            insert.Prepare();

            var defs = new Dictionary<string, Entry>();
            foreach (var line in lines)
            {
                if (line.StartsWith("#"))
                {
                    continue;
                }

                var tokens = line.Split('\t');

                var simplified = GetOrDefault(tokens, 1);
                var traditional = GetOrDefault(tokens, 2);
                var pinyin = PinyinUtil.ConvertAccentedUnspacedToNumbered(GetOrDefault(tokens, 3));
                var key = (simplified ?? traditional) + pinyin;
                Entry entry;
                if (!defs.TryGetValue(key, out entry))
                {
                    entry = defs[key] = new Entry();
                }

                entry.Simplified = entry.Simplified ?? simplified;
                entry.Traditional = entry.Traditional ?? traditional;
                entry.Pinyin = entry.Pinyin ?? pinyin;
                var definition = GetOrDefault(tokens, 4);
                if (!string.IsNullOrWhiteSpace(definition))
                {
                    if (!string.IsNullOrWhiteSpace(entry.Definition))
                    {
                        entry.Definition += ", " + definition;
                    }
                    else
                    {
                        entry.Definition = definition.ToSentenceCase();
                    }
                }

                entry.AddPartOfSpeech(GetOrDefault(tokens, 5));
                entry.Concept = GetOrDefault(tokens, 7);
                entry.Topic = GetOrDefault(tokens, 9);
                entry.ParentTopic = GetOrDefault(tokens, 11);
                entry.Notes = GetOrDefault(tokens, 14);
            }

            foreach (var entry in defs.Values)
            {
                var p = insert.Parameters;
                p.Clear();
                p.AddWithValue("simplified", entry.Simplified);
                p.AddWithValue("traditional", entry.Traditional);
                p.AddWithValue("pinyin", entry.Pinyin);
                p.AddWithValue("definition", entry.Definition);

                // TODO: Encode PoS information in an int instead of a string.
                p.AddWithValue("part_of_speech", entry.PartOfSpeech);
                p.AddWithValue("concept", entry.Concept);
                p.AddWithValue("topic", entry.Topic);
                p.AddWithValue("parent_topic", entry.ParentTopic);
                p.AddWithValue("notes", entry.Notes);
                insert.ExecuteNonQuery();
            }

            insert.Dispose();
        }
    }
}