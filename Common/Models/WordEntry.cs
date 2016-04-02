namespace Common.Models
{
    using System.Collections.Generic;

    public class WordEntry
    {
        public List<WordDefinition> Definitions { get; set; }

        public Dictionary<string, object> Extras { get; set; }
    }
}