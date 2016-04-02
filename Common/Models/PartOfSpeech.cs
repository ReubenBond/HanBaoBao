namespace Common.Models
{
    using System;

    [Flags]
    public enum PartOfSpeech : ulong
    {
        Address = (long)1, 

        Adjective = (long)1 << 1, 

        Adverb = (long)1 << 2, 

        AuxiliaryVerb = (long)1 << 3, 

        BoundMorpheme = (long)1 << 4, 

        SetPhrase = (long)1 << 5, 

        City = (long)1 << 6, 

        Complement = (long)1 << 7, 

        Conjunction = (long)1 << 8, 

        Country = (long)1 << 9, 

        Date = (long)1 << 10, 

        Determiner = (long)1 << 11, 

        Directional = (long)1 << 12, 

        Expression = (long)1 << 13, 

        ForeignTerm = (long)1 << 14, 

        Geography = (long)1 << 15, 

        Idiom = (long)1 << 16, 

        Interjection = (long)1 << 17, 

        MeasureWord = (long)1 << 18, 

        Measurement = (long)1 << 19, 

        Name = (long)1 << 20, 

        Noun = (long)1 << 21, 

        Number = (long)1 << 22, 

        Numeral = (long)1 << 23, 

        Onomatopoeia = (long)1 << 24, 

        Ordinal = (long)1 << 25, 

        Organization = (long)1 << 26, 

        Particle = (long)1 << 27, 

        Person = (long)1 << 28, 

        Phonetic = (long)1 << 29, 

        Phrase = (long)1 << 30, 

        Place = (long)1 << 31, 

        Prefix = (long)1 << 32, 

        Preposition = (long)1 << 33, 

        Pronoun = (long)1 << 34, 

        ProperNoun = (long)1 << 35, 

        Quantity = (long)1 << 36, 

        Radical = (long)1 << 37, 

        Suffix = (long)1 << 38, 

        Temporal = (long)1 << 39, 

        Time = (long)1 << 40, 

        Verb = (long)1 << 41, 

        Plural = (long)1 << 41
    }
}