namespace Common.Models
{
    public class WordDefinitionNote
    {
        public int Id { get; set; }

        public int UpVotes { get; set; }

        public int DownVotes { get; set; }

        public string Note { get; set; }

        public string Author { get; set; }
    }
}