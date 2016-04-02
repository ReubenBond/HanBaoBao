namespace Common.Models
{
    public class UserEvent
    {
        public long UtcTicks { get; set; }
        public UserEventType Kind { get; }
    }

    public enum UserEventType
    {
        /// <summary>
        /// User looked up the definition for the specified word.
        /// </summary>
        LookupDefinition,

        /// <summary>
        /// User resegmented a word.
        /// </summary>
        ResegmentWord,

        /// <summary>
        /// User translated a phrase.
        /// </summary>
        TranslatePhrase,

        AddedWordToList,
        AddedPhraseToList,
        RemovedWordFromList,
        RemovedPhraseFromList,
        AddedNoteToWord,
        UpVotedDefinition,
        DownVotedDefinition,
        AddedDefinition
    }
}
