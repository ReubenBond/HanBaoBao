namespace DictionaryDbBuilder.Hsk.HskHskCom
{
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Text;
    using System.Text.RegularExpressions;

    using DictionaryDbBuilder.Utilities;

    public static class HskHskComWordListImporter
    {
        public static List<WordList> Lists { get; } = new List<WordList>
                                                          {
                                                              new WordList(1, "1.txt"), 
                                                              new WordList(2, "2.txt"), 
                                                              new WordList(3, "3.txt"), 
                                                              new WordList(4, "4.txt"), 
                                                              new WordList(5, "5.txt"), 
                                                              new WordList(6, "6.txt")
                                                          };

        public static void UpdateDatabase(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var op =
                new SQLiteCommand(
                    "UPDATE OR IGNORE dictionary SET hsk_level=@hsk_level where simplified=@simplified and pinyin=@pinyin", 
                    connection, 
                    transaction);
            op.Prepare();
            foreach (var list in Lists)
            {
                foreach (var entry in list.Entries)
                {
                    op.Parameters.AddWithValue("hsk_level", list.Level);
                    op.Parameters.AddWithValue("pinyin", entry.Pinyin);
                    op.Parameters.AddWithValue("simplified", entry.Simplified);
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
            foreach (var line in typeof(HskHskComWordListImporter).ReadLines(fileName, Encoding.UTF8))
            {
                var row = line.Split('\t');
                yield return new Entry { Simplified = row[0], Traditional = row[1], Pinyin = SplitPinyin(row[2]) };
            }
        }

        private static string SplitPinyin(string input) => PinyinSplit.Replace(input, x => x + " ");
    }
}