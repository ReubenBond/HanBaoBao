namespace DictionaryDbBuilder.Utilities.StartDict
{
    using System.IO;
    using System.Linq;

    public class StarDictInfo
    {
        public readonly string Author = string.Empty;

        public readonly string Date = string.Empty;

        public readonly string Description = string.Empty;

        public readonly string EMail = string.Empty;

        public readonly string FileName;

        public readonly int IndexFileSize;

        public readonly string Name;

        public readonly int NumberOfEntries;

        public readonly int NumberOfSynonyms;

        public readonly int PointerSize = 4;

        public readonly char TypeMark;

        public readonly string Version;

        public readonly string WebSite = string.Empty;

        public StarDictInfo(string path)
        {
            this.FileName = path;
            this.BaseName = Path.Combine(
                Path.GetFullPath(Path.GetDirectoryName(path)), 
                Path.GetFileNameWithoutExtension(path));
            foreach (
                var term in
                    File.ReadAllLines(path)
                        .Select(l => l.Split('=').Select(it => it.Trim()).ToArray())
                        .Where(t => t.Length == 2))
            {
                switch (term[0])
                {
                    case "bookname":
                        this.Name = term[1];
                        break;
                    case "version":
                        this.Version = term[1];
                        break;
                    case "wordcount":
                        this.NumberOfEntries = int.Parse(term[1]);
                        break;
                    case "idxfilesize":
                        this.IndexFileSize = int.Parse(term[1]);
                        break;
                    case "author":
                        this.Author = term[1];
                        break;
                    case "email":
                        this.EMail = term[1];
                        break;
                    case "date":
                        this.Date = term[1];
                        break;
                    case "description":
                        this.Description = term[1];
                        break;
                    case "sametypesequence":
                        this.TypeMark = term[1][0];
                        break;
                    case "idxoffsetbits":
                        this.PointerSize = term[1] == "64" ? 8 : 4;
                        break;
                    case "synwordcount":
                        this.NumberOfSynonyms = int.Parse(term[1]);
                        break;
                }
            }

            if (this.PointerSize == 0)
            {
                this.PointerSize = 2;
            }
        }

        public string BaseName { get; private set; }

        public bool IsValid
            =>
                this.Name != null && this.NumberOfEntries != 0 && this.IndexFileSize != 0
                && (this.NumberOfSynonyms == 0 || File.Exists(Path.GetFileNameWithoutExtension(this.FileName) + ".syn"))
            ;
    }
}