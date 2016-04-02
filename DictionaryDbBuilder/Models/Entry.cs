namespace DictionaryDbBuilder
{
    using Common.Models;

    using DictionaryDbBuilder.Utilities;

    public class Entry
    {
        public string Concept { get; set; }

        public string Definition { get; set; }

        public string Notes { get; set; }

        public string ParentTopic { get; set; }

        public PartOfSpeech PartOfSpeech { get; set; }

        public string Pinyin { get; set; }

        public string Simplified { get; set; }

        public string Topic { get; set; }

        public string Traditional { get; set; }

        public void AddPartOfSpeech(string partOfSpeech)
        {
            if (string.IsNullOrWhiteSpace(partOfSpeech))
            {
                return;
            }

            this.PartOfSpeech = PartOfSpeechParser.ParsePartOfSpeech(partOfSpeech, this.PartOfSpeech);
        }

        public void Clear()
        {
            this.Traditional = null;
            this.Simplified = null;
            this.PartOfSpeech = 0;
            this.Pinyin = null;
            this.Definition = null;
            this.Concept = null;
            this.Topic = null;
            this.ParentTopic = null;
            this.Notes = null;
        }

        public bool IsValidSimplifiedOrTraditional(string original)
        {
            if (original == null)
            {
                return true; // Nulls are fine
            }

            if (original.Contains("_"))
            {
                return false;
            }

            return true;
        }

        public bool ShouldDropEntry(string partOfSpeech)
        {
            // TODO: Work out how to include 'patterns' from the NTI Dictionary.
            // TODO: Patterns have _ wildcards in them.
            return partOfSpeech.Contains("PUNCT") || partOfSpeech.Contains("ROMAN");
        }
    }
}