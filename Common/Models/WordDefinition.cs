namespace Common.Models
{
    using System.Collections.Generic;

    public class WordDefinition
    {
        public int Id { get; set; }

        public int UpVotes { get; set; }

        public int DownVotes { get; set; }

        public PartOfSpeech PartOfSpeech { get; set; }

        public string Language { get; set; }

        public string Definition { get; set; }

        public List<WordDefinitionNote> Notes { get; set; }
    }

    public class AddWordDefinition
    {
        public string SouceLanguage { get; set; }

        public PartOfSpeech PartOfSpeech { get; set; }

        public string DefinitionLanguage { get; set; }

        public string Definition { get; set; }
    }
}