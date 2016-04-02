namespace DictionaryDbBuilder.StartDictDictionaries
{
    using System;
    using System.Data.SQLite;
    using System.IO;
    using System.Linq;

    using DictionaryDbBuilder.Utilities;
    using DictionaryDbBuilder.Utilities.StartDict;

    public static class StarDictDictionaries
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
                    new[] { typeof(StarDictDictionaries).GetAssemblyPath() }.Concat(
                        typeof(StarDictDictionaries).Namespace.Split('.').Skip(1)).ToArray());
            foreach (var dictionaryFolder in Directory.EnumerateDirectories(Path.Combine(folder, "Dictionaries")))
            {
                var infoFile = Directory.EnumerateFiles(dictionaryFolder).FirstOrDefault(_ => _.EndsWith(".ifo"));
                if (infoFile == null)
                {
                    continue;
                }

                var dict = StarDict.TryOpen(infoFile.Substring(0, infoFile.Length - 4));

                foreach (var result in dict.Search(string.Empty))
                {
                    Console.WriteLine(result);
                }
            }

            /*
                const string sql = @"INSERT OR REPLACE INTO dictionary
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
        COALESCE(old.part_of_speech, new.part_of_speech),
        old.frequency,
        COALESCE(old.concept, new.concept),
        COALESCE(old.topic, new.topic),
        COALESCE(old.parent_topic, new.parent_topic),
        COALESCE(old.notes, new.notes)
    FROM (SELECT @simplified AS simplified, @traditional AS traditional, @pinyin AS pinyin, @definition AS definition, @part_of_speech as part_of_speech, @concept as concept, @topic as topic, @parent_topic as parent_topic, @notes as notes) AS new
    LEFT JOIN (SELECT * FROM dictionary WHERE simplified=@simplified and simplified not null and pinyin=@pinyin)
    AS old ON new.simplified = old.simplified and new.pinyin = old.pinyin";

                var insert = new SQLiteCommand(sql, connection, transaction);
                insert.Prepare();

                var defs = new Dictionary<string, Entry>();
                foreach (var line in lines)
                {
                    if (line.StartsWith("#")) continue;
                    var tokens = line.Split('\t');

                    var simplified = tokens[1];
                    var pinyin = PinyinUtil.ConvertAccentedUnspacedToNumbered(tokens[3]);
                    var key = simplified + pinyin;
                    Entry entry;
                    if (!defs.TryGetValue(key, out entry))
                    {
                        entry = defs[key] = new Entry();
                    }

                    entry.Simplified = entry.Simplified ?? simplified;
                    entry.Traditional = entry.Traditional ?? tokens[2];
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

                    entry.PartOfSpeech = GetOrDefault(tokens, 5);
                    entry.Concept = GetOrDefault(tokens, 7);
                    entry.Topic = GetOrDefault(tokens, 9);
                    entry.ParentTopic = GetOrDefault(tokens, 11);
                    entry.Notes = GetOrDefault(tokens, 14);
                }

                foreach (var entry in defs.Values)
                {
                    var p = insert.Parameters;
                    p.Clear();
                    p.AddIfSet("simplified", entry.Simplified);
                    p.AddIfSet("traditional", entry.Traditional);
                    p.AddIfSet("pinyin", entry.Pinyin);
                    p.AddIfSet("definition", entry.Definition);

                    // TODO: Encode PoS information in an int instead of a string.
                    p.AddIfSet("part_of_speech", entry.PartOfSpeech);
                    p.AddIfSet("concept", entry.Concept);
                    p.AddIfSet("topic", entry.Topic);
                    p.AddIfSet("parent_topic", entry.ParentTopic);
                    p.AddIfSet("notes", entry.Notes);
                    ++modifiedRows;
                    insert.ExecuteNonQuery();
                }
                
            }*/
        }
    }
}