namespace DictionaryDbBuilder.Utilities
{
    using System.Collections.Generic;
    using System.Data.SQLite;
    using System.Globalization;
    using System.Linq;
    using System.Text;

    public static class PinyinUtil
    {
        private const string AccentedPinyinTone3Supplement = "ĂăĔĕĬĭŎŏŬŭ";

        private const string AllAccentedPinyinCharacters = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜǖǘǚǜĀÁǍÀĒÉĚÈĪÍǏÌŌÓǑÒŪÚǓÙǕǗǙǛǕǗǙǛ";

        private const string UnaccentedPinyinTone3Supplement = "AaEeIiOoUu";

        /// <summary>
        ///     The set of all valid pinyin syllables.
        /// </summary>
        private static readonly HashSet<string> Syllables = new HashSet<string>
                                                               {
                                                                   "a", 
                                                                   "ba", 
                                                                   "pa", 
                                                                   "ma", 
                                                                   "fa", 
                                                                   "da", 
                                                                   "ta", 
                                                                   "na", 
                                                                   "la", 
                                                                   "ga", 
                                                                   "ka", 
                                                                   "ha", 
                                                                   "zha", 
                                                                   "cha", 
                                                                   "sha", 
                                                                   "za", 
                                                                   "ca", 
                                                                   "sa", 
                                                                   "ai", 
                                                                   "bai", 
                                                                   "pai", 
                                                                   "mai", 
                                                                   "dai", 
                                                                   "tai", 
                                                                   "nai", 
                                                                   "lai", 
                                                                   "gai", 
                                                                   "kai", 
                                                                   "hai", 
                                                                   "zhai", 
                                                                   "chai", 
                                                                   "shai", 
                                                                   "zai", 
                                                                   "cai", 
                                                                   "sai", 
                                                                   "an", 
                                                                   "ban", 
                                                                   "pan", 
                                                                   "man", 
                                                                   "fan", 
                                                                   "dan", 
                                                                   "tan", 
                                                                   "nan", 
                                                                   "lan", 
                                                                   "gan", 
                                                                   "kan", 
                                                                   "han", 
                                                                   "zhan", 
                                                                   "chan", 
                                                                   "shan", 
                                                                   "ran", 
                                                                   "zan", 
                                                                   "can", 
                                                                   "san", 
                                                                   "ang", 
                                                                   "bang", 
                                                                   "pang", 
                                                                   "mang", 
                                                                   "fang", 
                                                                   "dang", 
                                                                   "tang", 
                                                                   "nang", 
                                                                   "lang", 
                                                                   "gang", 
                                                                   "kang", 
                                                                   "hang", 
                                                                   "zhang", 
                                                                   "chang", 
                                                                   "shang", 
                                                                   "rang", 
                                                                   "zang", 
                                                                   "cang", 
                                                                   "sang", 
                                                                   "ao", 
                                                                   "bao", 
                                                                   "pao", 
                                                                   "mao", 
                                                                   "dao", 
                                                                   "tao", 
                                                                   "nao", 
                                                                   "lao", 
                                                                   "gao", 
                                                                   "kao", 
                                                                   "hao", 
                                                                   "zhao", 
                                                                   "chao", 
                                                                   "shao", 
                                                                   "rao", 
                                                                   "zao", 
                                                                   "cao", 
                                                                   "sao", 
                                                                   "e", 
                                                                   "me", 
                                                                   "de", 
                                                                   "te", 
                                                                   "ne", 
                                                                   "le", 
                                                                   "ge", 
                                                                   "ke", 
                                                                   "he", 
                                                                   "zhe", 
                                                                   "che", 
                                                                   "she", 
                                                                   "re", 
                                                                   "ze", 
                                                                   "ce", 
                                                                   "se", 
                                                                   "ei", 
                                                                   "bei", 
                                                                   "pei", 
                                                                   "mei", 
                                                                   "fei", 
                                                                   "dei", 
                                                                   "nei", 
                                                                   "lei", 
                                                                   "gei", 
                                                                   "hei", 
                                                                   "shei", 
                                                                   "zei", 
                                                                   "en", 
                                                                   "ben", 
                                                                   "pen", 
                                                                   "men", 
                                                                   "fen", 
                                                                   "den", 
                                                                   "nen", 
                                                                   "gen", 
                                                                   "ken", 
                                                                   "hen", 
                                                                   "zhen", 
                                                                   "chen", 
                                                                   "shen", 
                                                                   "ren", 
                                                                   "zen", 
                                                                   "cen", 
                                                                   "sen", 
                                                                   "beng", 
                                                                   "peng", 
                                                                   "meng", 
                                                                   "feng", 
                                                                   "deng", 
                                                                   "teng", 
                                                                   "neng", 
                                                                   "leng", 
                                                                   "geng", 
                                                                   "keng", 
                                                                   "heng", 
                                                                   "zheng", 
                                                                   "cheng", 
                                                                   "sheng", 
                                                                   "reng", 
                                                                   "zeng", 
                                                                   "ceng", 
                                                                   "seng", 
                                                                   "er", 
                                                                   "yi", 
                                                                   "bi", 
                                                                   "pi", 
                                                                   "mi", 
                                                                   "di", 
                                                                   "ti", 
                                                                   "ni", 
                                                                   "li", 
                                                                   "ji", 
                                                                   "qi", 
                                                                   "xi", 
                                                                   "zhi", 
                                                                   "chi", 
                                                                   "shi", 
                                                                   "ri", 
                                                                   "zi", 
                                                                   "ci", 
                                                                   "si", 
                                                                   "ya", 
                                                                   "dia", 
                                                                   "lia", 
                                                                   "jia", 
                                                                   "qia", 
                                                                   "xia", 
                                                                   "yan", 
                                                                   "bian", 
                                                                   "pian", 
                                                                   "mian", 
                                                                   "dian", 
                                                                   "tian", 
                                                                   "nian", 
                                                                   "lian", 
                                                                   "jian", 
                                                                   "qian", 
                                                                   "xian", 
                                                                   "yang", 
                                                                   "niang", 
                                                                   "liang", 
                                                                   "jiang", 
                                                                   "qiang", 
                                                                   "xiang", 
                                                                   "yao", 
                                                                   "biao", 
                                                                   "piao", 
                                                                   "miao", 
                                                                   "diao", 
                                                                   "tiao", 
                                                                   "niao", 
                                                                   "liao", 
                                                                   "jiao", 
                                                                   "qiao", 
                                                                   "xiao", 
                                                                   "ye", 
                                                                   "bie", 
                                                                   "pie", 
                                                                   "mie", 
                                                                   "die", 
                                                                   "tie", 
                                                                   "nie", 
                                                                   "lie", 
                                                                   "jie", 
                                                                   "qie", 
                                                                   "xie", 
                                                                   "yin", 
                                                                   "bin", 
                                                                   "pin", 
                                                                   "min", 
                                                                   "nin", 
                                                                   "lin", 
                                                                   "jin", 
                                                                   "qin", 
                                                                   "xin", 
                                                                   "ying", 
                                                                   "bing", 
                                                                   "ping", 
                                                                   "ming", 
                                                                   "ding", 
                                                                   "ting", 
                                                                   "ning", 
                                                                   "ling", 
                                                                   "jing", 
                                                                   "qing", 
                                                                   "xing", 
                                                                   "yo", 
                                                                   "yong", 
                                                                   "jiong", 
                                                                   "qiong", 
                                                                   "xiong", 
                                                                   "you", 
                                                                   "miu", 
                                                                   "diu", 
                                                                   "niu", 
                                                                   "liu", 
                                                                   "jiu", 
                                                                   "qiu", 
                                                                   "xiu", 
                                                                   "o", 
                                                                   "bo", 
                                                                   "po", 
                                                                   "mo", 
                                                                   "fo", 
                                                                   "lo", 
                                                                   "weng", 
                                                                   "dong", 
                                                                   "tong", 
                                                                   "nong", 
                                                                   "long", 
                                                                   "gong", 
                                                                   "kong", 
                                                                   "hong", 
                                                                   "zhong", 
                                                                   "chong", 
                                                                   "rong", 
                                                                   "zong", 
                                                                   "cong", 
                                                                   "song", 
                                                                   "ou", 
                                                                   "pou", 
                                                                   "mou", 
                                                                   "fou", 
                                                                   "dou", 
                                                                   "tou", 
                                                                   "nou", 
                                                                   "lou", 
                                                                   "gou", 
                                                                   "kou", 
                                                                   "hou", 
                                                                   "zhou", 
                                                                   "chou", 
                                                                   "shou", 
                                                                   "rou", 
                                                                   "zou", 
                                                                   "cou", 
                                                                   "sou", 
                                                                   "wu", 
                                                                   "bu", 
                                                                   "pu", 
                                                                   "mu", 
                                                                   "fu", 
                                                                   "du", 
                                                                   "tu", 
                                                                   "nu", 
                                                                   "lu", 
                                                                   "gu", 
                                                                   "ku", 
                                                                   "hu", 
                                                                   "zhu", 
                                                                   "chu", 
                                                                   "shu", 
                                                                   "ru", 
                                                                   "zu", 
                                                                   "cu", 
                                                                   "su", 
                                                                   "wa", 
                                                                   "gua", 
                                                                   "kua", 
                                                                   "hua", 
                                                                   "zhua", 
                                                                   "shua", 
                                                                   "wai", 
                                                                   "guai", 
                                                                   "kuai", 
                                                                   "huai", 
                                                                   "chuai", 
                                                                   "shuai", 
                                                                   "wan", 
                                                                   "duan", 
                                                                   "tuan", 
                                                                   "nuan", 
                                                                   "luan", 
                                                                   "guan", 
                                                                   "kuan", 
                                                                   "huan", 
                                                                   "zhuan", 
                                                                   "chuan", 
                                                                   "shuan", 
                                                                   "ruan", 
                                                                   "zuan", 
                                                                   "cuan", 
                                                                   "suan", 
                                                                   "wang", 
                                                                   "guang", 
                                                                   "kuang", 
                                                                   "huang", 
                                                                   "zhuang", 
                                                                   "chuang", 
                                                                   "shuang", 
                                                                   "yue", 
                                                                   "nüe", 
                                                                   "lüe", 
                                                                   "jue", 
                                                                   "que", 
                                                                   "xue", 
                                                                   "wei", 
                                                                   "dui", 
                                                                   "tui", 
                                                                   "gui", 
                                                                   "kui", 
                                                                   "hui", 
                                                                   "zhui", 
                                                                   "chui", 
                                                                   "shui", 
                                                                   "rui", 
                                                                   "zui", 
                                                                   "cui", 
                                                                   "sui", 
                                                                   "wen", 
                                                                   "dun", 
                                                                   "tun", 
                                                                   "lun", 
                                                                   "gun", 
                                                                   "kun", 
                                                                   "hun", 
                                                                   "zhun", 
                                                                   "chun", 
                                                                   "shun", 
                                                                   "run", 
                                                                   "zun", 
                                                                   "cun", 
                                                                   "sun", 
                                                                   "wo", 
                                                                   "duo", 
                                                                   "tuo", 
                                                                   "nuo", 
                                                                   "luo", 
                                                                   "guo", 
                                                                   "kuo", 
                                                                   "huo", 
                                                                   "zhuo", 
                                                                   "chuo", 
                                                                   "shuo", 
                                                                   "ruo", 
                                                                   "zuo", 
                                                                   "cuo", 
                                                                   "suo", 
                                                                   "yu", 
                                                                   "nü", 
                                                                   "lü", 
                                                                   "ju", 
                                                                   "qu", 
                                                                   "xu", 
                                                                   "yuan", 
                                                                   "juan", 
                                                                   "quan", 
                                                                   "xuan", 
                                                                   "yun", 
                                                                   "jun", 
                                                                   "qun", 
                                                                   "xun"
                                                               };

        private static readonly Dictionary<char, string> AccentLookup = new Dictionary<char, string>
                                                                            {
                                                                                { 'a', "āáǎàa" }, 
                                                                                { 'e', "ēéěèe" }, 
                                                                                { 'i', "īíǐìi" }, 
                                                                                { 'o', "ōóǒòo" }, 
                                                                                { 'u', "ūúǔùu" }, 
                                                                                { 'ü', "ǖǘǚǜü" }, 
                                                                                { 'v', "ǖǘǚǜü" }, 
                                                                                { 'A', "ĀÁǍÀA" }, 
                                                                                { 'E', "ĒÉĚÈE" }, 
                                                                                { 'I', "ĪÍǏÌI" }, 
                                                                                { 'O', "ŌÓǑÒO" }, 
                                                                                { 'U', "ŪÚǓÙU" }, 
                                                                                { 'Ü', "ǕǗǙǛÜ" }, 
                                                                                { 'V', "ǕǗǙǛÜ" }
                                                                            };

        private static readonly KeyValuePair<char, string>[] Accents = AccentLookup.ToArray();

        private static readonly int MaxSyllableLength = Syllables.Max(_ => _.Length);

        public static void InsertMissingPinyin(SQLiteConnection connection, SQLiteTransaction transaction)
        {
            var cmd = new SQLiteCommand("select rowid, simplified, traditional from dictionary where pinyin is null", connection, transaction);

            var insertion = new SQLiteCommand(
                "update dictionary set pinyin=@pinyin where rowid = @rowid",
                connection,
                transaction);
            
                insertion.Prepare();
                using (var missing = cmd.ExecuteReader())
                {
                    while (missing.Read())
                    {
                        var text = missing.GetString(1) ?? missing.GetString(2);
                        var pinyin = GetProbablePinyin(connection, transaction, text);
                        if (string.IsNullOrWhiteSpace(pinyin))
                        {
                            continue;
                        }

                        insertion.Parameters.AddWithValue("rowid", missing.GetValue(0));
                        insertion.Parameters.AddWithValue("pinyin", pinyin);
                        insertion.ExecuteNonQuery();
                    }
                }

                insertion.Dispose();
                cmd.Dispose();
            
        }

        /// <summary>
        /// Returns the probable pinyin for the sequence of characters provided.
        /// </summary>
        /// <param name="connection">The database connection</param>
        /// <param name="transaction">The database transaction.</param>
        /// <param name="chars">The sequence of characters.</param>
        /// <returns>The probable pinyin for the sequence of characters provided.</returns>
        public static string GetProbablePinyin(SQLiteConnection connection, SQLiteTransaction transaction, string chars)
        {
            if (string.IsNullOrWhiteSpace(chars)) return null;
            var result = new StringBuilder();
            for (var i = 0; i < chars.Length; i++)
            {
                var current = LookupPinyin(connection, transaction, chars[i]);
                if (string.IsNullOrWhiteSpace(current)) return null;
                if (i > 0) result.Append(' ');
                result.Append(current);
            }

            return result.ToString();
        }

        /// <summary>
        /// Searches the database for the pinyin for the provided character, returning <see langword="null"/> if not found.
        /// </summary>
        /// <param name="connection">The database connection.</param>
        /// <param name="transaction">The database transaction.</param>
        /// <param name="c">The character to search for.</param>
        /// <returns>The pinyin for the provided character, or <see langword="null"/> if not found.</returns>
        public static string LookupPinyin(SQLiteConnection connection, SQLiteTransaction transaction, char c)
        {
            using (
                var cmd =
                    new SQLiteCommand(
                        "select pinyin from dictionary where simplified = @c or traditional = @c order by frequency desc limit 1",
                        connection,
                        transaction))
            {
                cmd.Parameters.AddWithValue("c", c.ToString());
                using (var reader = cmd.ExecuteReader())
                {
                    if (!reader.Read()) return null;
                    var result = reader.GetValue(0);
                    return result as string;
                }
            }
        }

        public static string ConvertAccentedToNumbered(string input)
        {
            var result = new StringBuilder();
            ConvertAccentedToNumbered(input, result);
            return result.ToString();
        }

        /// <summary>
        ///     Converts accented pinyin with no spaces into numbered pinyin.
        /// </summary>
        /// <param name="input">The input.</param>
        /// <param name="ensureSpaceBetweenSyllables">
        ///     Ensures that each syllable is spaced. When enabled, no other punctuation is
        ///     emitted.
        /// </param>
        /// <returns>The tone-numbered pinyin.</returns>
        public static string ConvertAccentedUnspacedToNumbered(string input, bool ensureSpaceBetweenSyllables = true)
        {
            if (input == null)
            {
                return null;
            }

            input = input.Replace(" ", string.Empty);
            var unaccented = GetUnaccented(input);
            var spans = new List<int>();
            if (FindValidPinyinSpans(input, unaccented, spans, 0))
            {
                // success
                var result = new StringBuilder();
                var start = 0;
                foreach (var length in spans)
                {
                    if (ensureSpaceBetweenSyllables && start > 0)
                    {
                        result.Append(' ');
                    }

                    ConvertAccentedToNumbered(input, result, start, start + length);
                    start += length;

                    // Add non-pinyin characters verbatim.
                    while (start < input.Length)
                    {
                        if (IsUnmarkedPinyinLetter(unaccented[start]))
                        {
                            break;
                        }

                        if (!ensureSpaceBetweenSyllables)
                        {
                            result.Append(input[start]);
                        }

                        start++;
                    }
                }

                return result.ToString();
            }

            return null;
        }

        public static int GetAccentedPinyinCharacterTone(char c)
        {
            for (var i = 0; i < AllAccentedPinyinCharacters.Length; i++)
            {
                var lookupChar = AllAccentedPinyinCharacters[i];
                if (c == lookupChar)
                {
                    return (i % 4) + 1;
                }
            }

            // Some converters apparently output a weird representation of tone 3
            if (AccentedPinyinTone3Supplement.Any(lookupChar => c == lookupChar))
            {
                return 3;
            }

            return -1;
        }

        public static string NormalizeNumbered(string input, StringBuilder builder)
        {
            builder.Clear();
            var wasDigit = false;
            for (var i = 0; i < input.Length; i++)
            {
                // If a digit was encountered and was not followed by a space, insert a space.
                if (wasDigit && input[i] != ' ')
                {
                    // If this is the first time this case has been encountered, copy all previous characters.
                    if (builder.Length == 0)
                    {
                        builder.Append(input.Substring(0, i));
                    }

                    builder.Append(' ');
                }

                if (builder.Length > 0)
                {
                    builder.Append(input[i]);
                }

                wasDigit = char.IsDigit(input[i]);
            }

            if (builder.Length > 0)
            {
                return builder.ToString();
            }

            return input;
        }

        private static void ConvertAccentedToNumbered(string input, StringBuilder result, int start = 0, int end = -1)
        {
            if (input == null)
            {
                return;
            }

            if (end < 0)
            {
                end = input.Length;
            }

            var tone = -1;
            for (var i = start; i < end; i++)
            {
                var c = input[i];
                if (c == ' ')
                {
                    result.Append(tone > 0 ? tone : 5);
                    tone = -1;
                }

                var newTone = GetAccentedPinyinCharacterTone(c);
                if (newTone == -1)
                {
                    result.Append(c);
                }
                else
                {
                    tone = newTone;
                    var replacement = GetUnaccentedCharacter(c);
                    result.Append(replacement);
                }
            }

            result.Append(tone > 0 ? tone : 5);
        }

        private static int FindLongestMatchingLength(string input, int start)
        {
            var length = start + MaxSyllableLength > input.Length ? input.Length - start : MaxSyllableLength;
            var unaccented = GetUnaccented(input);
            for (; length > 0; length--)
            {
                if (Syllables.Contains(unaccented.Substring(start, length)))
                {
                    return length;
                }
            }

            // Backtrack
            return length;
        }

        private static bool FindValidPinyinSpans(string input, string unaccented, List<int> result, int start)
        {
            // Check if done, return if so.
            if (start == input.Length)
            {
                return true;
            }

            foreach (var spanLength in GetPinyinSpans(input, unaccented, start))
            {
                // Try this span.
                result.Add(spanLength);

                // Skip over punctuation.
                var newStart = start + spanLength;
                for (var i = start + spanLength; i < input.Length; i++)
                {
                    if (IsUnmarkedPinyinLetter(unaccented[i]))
                    {
                        break;
                    }

                    newStart++;
                }

                if (FindValidPinyinSpans(input, unaccented, result, newStart))
                {
                    return true;
                }

                // Backtrack and try again.
                result.RemoveAt(result.Count - 1);
            }

            // Backtrack.
            return false;
        }

        private static IEnumerable<int> GetPinyinSpans(string input, string unaccented, int start)
        {
            // TODO: fix.... should account for non-accented, non-alpha characters.
            var length = start + MaxSyllableLength > input.Length ? input.Length - start : MaxSyllableLength;

            // Stop before any punctuation.
            for (var i = start; i < start + length; i++)
            {
                if (!IsUnmarkedPinyinLetter(unaccented[i]))
                {
                    length = i - start;
                    break;
                }
            }

            // Only allow up to one tone-marked character.
            var toneMarks = 0;
            for (var i = start; i < start + length; i++)
            {
                if (!IsUnmarkedPinyinLetter(input[i]))
                {
                    toneMarks++;

                    // If this is the second tone mark, ensure this index is not included.
                    if (toneMarks > 1)
                    {
                        length = i - start;
                        break;
                    }
                }
            }

            while (length > 0)
            {
                if (Syllables.Contains(unaccented.Substring(start, length)))
                {
                    yield return length;
                }

                length--;
            }
        }

        private static string GetUnaccented(string input)
        {
            var scratch = new StringBuilder();
            for (var i = 0; i < input.Length; i++)
            {
                scratch.Append(char.ToLowerInvariant(GetUnaccentedCharacter(input[i])));
            }

            return scratch.ToString();
        }

        private static char GetUnaccentedCharacter(char c)
        {
            for (var row = 0; row < Accents.Length; row++)
            {
                var currentRow = Accents[row];
                for (var col = 0; col < currentRow.Value.Length; col++)
                {
                    if (currentRow.Value[col] == c)
                    {
                        return currentRow.Key;
                    }
                }
            }

            for (var col = 0; col < AccentedPinyinTone3Supplement.Length; col++)
            {
                if (AccentedPinyinTone3Supplement[col] == c)
                {
                    return UnaccentedPinyinTone3Supplement[col];
                }
            }

            return c;
        }

        private static bool IsUnmarkedPinyinLetter(char c)
        {
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == 'ü' || c == 'Ü';
        }
    }
}