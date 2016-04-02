using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Text;

namespace JunDaCharacterFrequencyMunger
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length < 2)
            {
                Console.Error.WriteLine(
                    $"Usage {Assembly.GetEntryAssembly()} <directory> <outputFileName>\n"
                    +"Will load all .txt files in that directory as character frequency files, aggregate them, and output the result.");
                return;
            }

            var directoryName = args[0];
            if (!Directory.Exists(directoryName))
            {
                Console.Error.WriteLine($"Directory \"{directoryName}\" does not exist.");
                return;
            }

            var outputFileName = args[1];
            if (string.IsNullOrWhiteSpace(outputFileName))
            {
                Console.Error.Write("Output file name was not specified.");
                return;
            }

            var frequencies = new Dictionary<string, Entry>();
            foreach (var fileName in Directory.EnumerateFiles(directoryName, "*.txt"))
            {
                foreach (var line in File.ReadLines(fileName, Encoding.Unicode))
                {
                    if (string.IsNullOrWhiteSpace(line)) continue;
                    if (!char.IsDigit(line[0])) continue;
                    var splits = line.Split('\t');
                    if (splits.Length < 5) continue;
                    var word = splits[1];
                    var frequency = int.Parse(splits[2]);
                    var pinyin = splits[4];
                    Entry existing;
                    if (frequencies.TryGetValue(word, out existing) && string.Equals(existing.Pinyin, pinyin))
                    {
                        if (existing.Count < frequency) existing.Count = frequency;
                    }
                    else
                    {
                        frequencies[word] = new Entry {Count = frequency, Pinyin = pinyin};
                    }
                }
            }

            using (var file = File.Open(outputFileName, FileMode.Create))
                using (var outStream = new StreamWriter(file, Encoding.UTF8))
            {
                foreach (var pair in frequencies)
                {
                    outStream.WriteLine(pair.Key + "\t" + pair.Value.Pinyin + "\t" + pair.Value.Count);
                }
            }
        }
    }

    public class Entry
    {
        public int Count { get; set; }
        public string Pinyin { get; set; }
    }
}
