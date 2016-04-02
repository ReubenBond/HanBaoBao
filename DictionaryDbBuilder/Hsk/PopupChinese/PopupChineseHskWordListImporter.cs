namespace DictionaryDbBuilder.Hsk.PopupChinese
{
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Text.RegularExpressions;

    using DictionaryDbBuilder.Utilities;

    using Excel.Helper;

    public static class PopupChineseHskWordListImporter
    {
        public static List<WordList> Lists { get; } = new List<WordList>
                                                          {
                                                              new WordList(1, "NewHSKLevel1.xls"), 
                                                              new WordList(2, "NewHSKLevel2.xls"), 
                                                              new WordList(3, "NewHSKLevel3.xls"), 
                                                              new WordList(4, "NewHSKLevel4.xls"), 
                                                              new WordList(5, "NewHSKLevel5.xls"), 
                                                              new WordList(6, "NewHSKLevel6.xls")
                                                          };

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
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
        COALESCE(old.hsk_level, new.hsk_level),
        COALESCE(old.part_of_speech, 0) | new.part_of_speech,
        old.frequency
     FROM (SELECT @simplified AS simplified, @traditional AS traditional, @pinyin AS pinyin, @definition as definition, @hsk_level AS hsk_level, @part_of_speech as part_of_speech) AS new
     LEFT JOIN(SELECT * FROM dictionary WHERE simplified=@simplified and simplified not null and pinyin=@pinyin) AS old ON new.simplified = old.simplified and new.pinyin = old.pinyin";
            var op = new SQLiteCommand(Sql, connection, transaction);
            op.Prepare();

            foreach (var list in Lists)
            {
                foreach (var entry in list.Entries)
                {
                    op.Parameters.AddWithValue("hsk_level", list.Level);
                    op.Parameters.AddWithValue("part_of_speech", entry.PartOfSpeech);
                    op.Parameters.AddWithValue("pinyin", entry.Pinyin);
                    op.Parameters.AddWithValue("simplified", entry.Simplified);
                    op.Parameters.AddWithValue("traditional", entry.Traditional);
                    op.Parameters.AddWithValue("definition", entry.Definition);
                    op.ExecuteNonQuery();
                }
            }

            op.Dispose();
        }
    }

    public class WordList
    {
        private static readonly Regex PinyinSplit = new Regex(@"[0-9](?!\s|$)", RegexOptions.Compiled);

        private readonly string fileName;

        public WordList(int level, string fileName)
        {
            this.fileName = fileName;
            this.Level = level;
        }

        public IEnumerable<Entry> Entries => GetHskListForLevel(this.fileName);

        public int Level { get; set; }

        private static IEnumerable<Entry> GetHskListForLevel(string fileName)
        {
            using (var list = typeof(PopupChineseHskWordListImporter).GetEmbeddedFile(fileName))
            {
                var helper = new ExcelDataReaderHelper(list);
                foreach (var row in helper.GetRangeCells(0, 2, 2, 5))
                {
                    var result = new Entry
                                     {
                                         Simplified = (string)row[0], 
                                         Traditional = (string)row[1], 
                                         Pinyin = SplitPinyin((string)row[2]), 
                                         Definition = (string)row[3]
                                     };
                    result.AddPartOfSpeech((string)row[4]);
                    yield return result;
                }
            }
        }

        private static string SplitPinyin(string input) => PinyinSplit.Replace(input, x => x + " ");
    }
}