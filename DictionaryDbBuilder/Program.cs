namespace DictionaryDbBuilder
{
    using System;
    using System.Data.SQLite;
    using System.IO;

    using Adso;
    using CcCedict;
    using Hsk.HskHskCom;
    using Hsk.PopupChinese;
    using NtiBuddhistDictionary;
    using Unihan;
    using Utilities;
    using WordFrequency;

    internal class Program
    {
        private static void Main(string[] args)
        {
            const string DatabaseFileName = "hanbaobao.db";
            const string RelativeOutputDirectory = @"..\..\..\HanBaoBao\app\src\main\assets\databases";
            SQLiteConnection.CreateFile(DatabaseFileName);
            var connection = new SQLiteConnection($"Data Source={DatabaseFileName};Version=3");
            connection.Open();

            using (var transaction = connection.BeginTransaction())
            {
                // Create the database.
                var creationScript = typeof(Program).GetEmbeddedFileContents("create.sql");
                new SQLiteCommand(creationScript, connection, transaction).ExecuteNonQuery();

                // Populate the data.

                // StarDictDictionaries.UpdateDatabase(connection);
                /*TatoebaSentenceImporter.UpdateDatabase(connection)
                                    +*/
                CcCedictImporter.UpdateDatabase(connection, transaction);
                NtiBuddhistDictionaryImporter.UpdateDatabase(connection, transaction);
                AdsoTransImporter.UpdateDatabase(connection, transaction);
                UnihanImporter.UpdateDatabase(connection, transaction);
                PopupChineseHskWordListImporter.UpdateDatabase(connection, transaction);
                HskHskComWordListImporter.UpdateDatabase(connection, transaction);
                WordFrequencyImporter.UpdateDatabase(connection, transaction);

                // Fix pinyin for missing entries
                PinyinUtil.InsertMissingPinyin(connection, transaction);
                
                // Remove (almost) useless terms. We don't want to segment into words which don't have any definition,
                // since it doesn't provide much value and we cannot be certain that those words actually exist.
                new SQLiteCommand(
                    "delete from dictionary where definition is null or pinyin is null",
                    connection,
                    transaction).ExecuteNonQuery();

                transaction.Commit();
            }

            // Set the version and clean up the redundant indices.
            var version = (int)(DateTime.UtcNow - new DateTime(2000, 1, 1)).TotalDays + 1;
            new SQLiteCommand(typeof(Program).GetEmbeddedFileContents("finalize.sql"), connection).ExecuteNonQuery();
            new SQLiteCommand($@"PRAGMA user_version='{version}'", connection).ExecuteNonQuery();
            var outputFile = connection.FileName;
            var includedLines = (long)new SQLiteCommand("select count(*) from dictionary", connection).ExecuteScalar();
            connection.Close();
            Console.WriteLine($"Done! Inserted {includedLines} entries!");

            // Delete existing versions of this database in the output directory.
            var outputDir = Path.Combine(Environment.CurrentDirectory, RelativeOutputDirectory);
            foreach (var fileName in Directory.EnumerateFiles(outputDir))
            {
                var name = Path.GetFileName(fileName);
                if (name == null)
                {
                    continue;
                }

                if (name.StartsWith(DatabaseFileName))
                {
                    File.Delete(fileName);
                }
            }

            // Copy the database to the output directory.
            File.Copy(
                outputFile, 
                Path.Combine(Environment.CurrentDirectory, RelativeOutputDirectory, $"{DatabaseFileName}.{version}"));

            Console.ReadKey();
        }
    }
}