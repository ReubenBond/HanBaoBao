//#define ONLY_INCLUDE_ENTRIES_WITH_DEFINITIONS
namespace DictionaryDbBuilder.Unihan
{
    using System;
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Globalization;
    using System.Text;
    using System.Text.RegularExpressions;

    using DictionaryDbBuilder.Utilities;

    public static class UnihanImporter
    {
        private static readonly Regex DefinitionReplace = new Regex(
            @"(\([^)]+U\+[^)]+\)\s*)", 
            RegexOptions.Compiled | RegexOptions.CultureInvariant);

        private static readonly Regex LinePattern = new Regex(
            @"^U\+([^\s]+)\t([^\s]+)\t(.+)$", 
            RegexOptions.Compiled | RegexOptions.CultureInvariant);

        private static readonly Regex VariantPattern = new Regex(
            @"^U\+(2?[0-9A-F]{4})\t([^\s]+)\tU\+(2?[0-9A-F]{4})$", 
            RegexOptions.Compiled | RegexOptions.CultureInvariant);

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var lines = typeof(UnihanImporter).ReadLines("Unihan_Readings.txt", Encoding.UTF8);
            var entries = Entries(lines);

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
        frequency
    )
    SELECT
        old.rowid,
        COALESCE(old.simplified, new.simplified),
        COALESCE(old.traditional, new.traditional),
        COALESCE(old.pinyin, new.pinyin),
        COALESCE(old.definition, new.definition),
        old.classifier,
        old.hsk_level,
        old.part_of_speech,
        old.frequency
     FROM (SELECT @simplified AS simplified, @traditional AS traditional, @pinyin AS pinyin, @definition AS definition) AS new
     LEFT JOIN(SELECT * FROM dictionary WHERE simplified=@simplified and simplified not null) AS old ON new.simplified = old.simplified and new.pinyin = old.pinyin";

            var insert = new SQLiteCommand(Sql, connection, transaction);
            insert.Prepare();

            var variantMap = GetCharVariantMappings();
            var simplifiedToTraditional = variantMap.Item1;
            var traditionalToSimplified = variantMap.Item2;
            foreach (var entry in entries)
            {
                insert.Parameters.Clear();
                var character = GetOrDefault(entry, "char");
                var pinyin = GetOrDefault(entry, "kMandarin");
                var definition = GetOrDefault(entry, "kDefinition");
                if (string.IsNullOrWhiteSpace(character) || string.IsNullOrWhiteSpace(pinyin)
#if ONLY_INCLUDE_ENTRIES_WITH_DEFINITIONS
                        || string.IsNullOrWhiteSpace(definition)
#endif
                    )
                {
                    continue;
                }

                // Find the simplified/traditional variants of this character.
                string simplified, traditional;
                if (simplifiedToTraditional.TryGetValue(character, out traditional))
                {
                    simplified = character;
                }
                else if (traditionalToSimplified.TryGetValue(character, out simplified))
                {
                    traditional = character;
                }

                if (string.IsNullOrWhiteSpace(traditional) && string.IsNullOrWhiteSpace(simplified))
                {
                    simplified = traditional = character;
                }

                // Stuff the values into the database.
                insert.Parameters.AddWithValue("simplified", simplified);
                insert.Parameters.AddWithValue("traditional", traditional);
                insert.Parameters.AddWithValue("pinyin", PinyinUtil.ConvertAccentedToNumbered(pinyin));
                insert.Parameters.AddWithValue("definition", CleanDefinition(definition).ToSentenceCase());

                insert.ExecuteNonQuery();
            }

            insert.Dispose();
        }

        private static string CleanDefinition(string input)
        {
            return input == null ? null : DefinitionReplace.Replace(input, string.Empty).Replace(";", ", ").Trim();
        }

        private static IEnumerable<Dictionary<string, string>> Entries(IEnumerable<string> lines)
        {
            string currentChar = null;
            var result = default(Dictionary<string, string>);
            foreach (var line in lines)
            {
                var match = LinePattern.Match(line);
                if (!match.Success)
                {
                    continue;
                }

                var codepoint = char.ConvertFromUtf32(int.Parse(match.Groups[1].Value, NumberStyles.HexNumber));
                if (codepoint != currentChar)
                {
                    if (result != null)
                    {
                        yield return result;
                    }

                    result = new Dictionary<string, string> { ["char"] = currentChar = codepoint };
                }

                result[match.Groups[2].Value] = match.Groups[3].Value;
            }

            if (result != null)
            {
                yield return result;
            }
        }

        private static Tuple<Dictionary<string, string>, Dictionary<string, string>> GetCharVariantMappings()
        {
            var simplifiedToTraditional = new Dictionary<string, string>();
            var traditionalToSimplified = new Dictionary<string, string>();
            foreach (var line in typeof(UnihanImporter).ReadLines("Unihan_Variants.txt", Encoding.UTF8))
            {
                var match = VariantPattern.Match(line);
                if (!match.Success)
                {
                    continue;
                }

                var codepoint = char.ConvertFromUtf32(int.Parse(match.Groups[1].Value, NumberStyles.HexNumber));
                var otherCodepoint = char.ConvertFromUtf32(int.Parse(match.Groups[3].Value, NumberStyles.HexNumber));
                if (match.Groups[2].Value == "kSimplifiedVariant")
                {
                    traditionalToSimplified[codepoint] = otherCodepoint;
                }
                else if (match.Groups[2].Value == "kTraditionalVariant")
                {
                    simplifiedToTraditional[codepoint] = otherCodepoint;
                }
            }

            return Tuple.Create(simplifiedToTraditional, traditionalToSimplified);
        }

        private static string GetOrDefault(Dictionary<string, string> dictionary, string key)
        {
            string result;
            dictionary.TryGetValue(key, out result);
            return result;
        }
    }
}