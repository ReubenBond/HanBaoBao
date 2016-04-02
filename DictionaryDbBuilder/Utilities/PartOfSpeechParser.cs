namespace DictionaryDbBuilder.Utilities
{
    using System.Text;

    using Common.Models;

    public static class PartOfSpeechParser
    {
        public static PartOfSpeech ParsePartOfSpeech(string pos, PartOfSpeech result)
        {
            if (pos == null)
            {
                return result;
            }

            pos = pos.ToLowerInvariant();

            // TODO: If this is too slow, use a parser generator.
            if (pos.Contains("address"))
            {
                result |= PartOfSpeech.Address;
            }

            if (pos.Contains("adj") || pos.Contains("XING"))
            {
                result |= PartOfSpeech.Adjective;
            }

            if (pos.Contains("adv") || pos.Contains("cad"))
            {
                result |= PartOfSpeech.Adverb;
            }
            else if (pos.Contains("aux"))
            {
                result |= PartOfSpeech.AuxiliaryVerb;
            }
            else if (pos.StartsWith("verb"))
            {
                result |= PartOfSpeech.Verb;
            }

            if (pos.Contains("bound form"))
            {
                result |= PartOfSpeech.BoundMorpheme;
            }

            if (pos.Contains("set") || pos.Contains("CHENGYU"))
            {
                result |= PartOfSpeech.SetPhrase;
            }
            else if (pos.Contains("phrase"))
            {
                result |= PartOfSpeech.Phrase;
            }

            if (pos.Contains("city"))
            {
                result |= PartOfSpeech.City;
            }

            if (pos.Contains("complement"))
            {
                result |= PartOfSpeech.Complement;
            }

            if (pos.Contains("conj"))
            {
                result |= PartOfSpeech.Conjunction;
            }

            if (pos.Contains("country"))
            {
                result |= PartOfSpeech.Country;
            }

            if (pos.Contains("date"))
            {
                result |= PartOfSpeech.Date;
            }

            if (pos.Contains("determiner"))
            {
                result |= PartOfSpeech.Determiner;
            }

            if (pos.Contains("directional"))
            {
                result |= PartOfSpeech.Directional;
            }

            if (pos.Contains("expression"))
            {
                result |= PartOfSpeech.Expression;
            }

            if (pos.Contains("foreign"))
            {
                result |= PartOfSpeech.ForeignTerm;
            }

            if (pos.Contains("geo"))
            {
                result |= PartOfSpeech.Geography;
            }

            if (pos.Contains("idiom"))
            {
                result |= PartOfSpeech.Idiom;
            }

            if (pos.Contains("interj"))
            {
                result |= PartOfSpeech.Interjection;
            }

            if (pos.Contains("measure word") || pos.Contains("mw"))
            {
                result |= PartOfSpeech.MeasureWord;
            }

            if (pos.Contains("measurement") || pos.Contains("mment"))
            {
                result |= PartOfSpeech.Measurement;
            }

            if (pos.Contains("name"))
            {
                result |= PartOfSpeech.Name;
            }

            if (pos.Contains("numeral"))
            {
                result |= PartOfSpeech.Numeral;
            }
            else if (pos.Contains("num"))
            {
                result |= PartOfSpeech.Number;
            }

            if (pos.Contains("onomat"))
            {
                result |= PartOfSpeech.Onomatopoeia;
            }

            if (pos.Contains("ord"))
            {
                result |= PartOfSpeech.Ordinal;
            }

            if (pos.Contains("org"))
            {
                result |= PartOfSpeech.Organization;
            }

            if (pos.Contains("particle"))
            {
                result |= PartOfSpeech.Particle;
            }

            if (pos.Contains("person"))
            {
                result |= PartOfSpeech.Person;
            }

            if (pos.Contains("phonetic"))
            {
                result |= PartOfSpeech.Phonetic;
            }

            if (pos.Contains("place"))
            {
                result |= PartOfSpeech.Place;
            }

            if (pos.Contains("prefix"))
            {
                result |= PartOfSpeech.Prefix;
            }

            if (pos.Contains("prep"))
            {
                result |= PartOfSpeech.Preposition;
            }

            if (pos.Contains("pron"))
            {
                result |= PartOfSpeech.Pronoun;
            }
            else if (pos.Contains("prop"))
            {
                result |= PartOfSpeech.ProperNoun;
            }
            else if (pos.Contains("noun"))
            {
                result |= PartOfSpeech.Noun;
            }

            if (pos.Contains("quantity"))
            {
                result |= PartOfSpeech.Quantity;
            }

            if (pos.Contains("radical"))
            {
                result |= PartOfSpeech.Radical;
            }

            if (pos.Contains("suffix"))
            {
                result |= PartOfSpeech.Suffix;
            }

            if (pos.Contains("temporal"))
            {
                result |= PartOfSpeech.Temporal;
            }

            if (pos.Contains("time"))
            {
                result |= PartOfSpeech.Time;
            }

            return result;
        }

        public static string PartOfSpeechToString(PartOfSpeech pos)
        {
            var naive = pos.ToString();
            var result = new StringBuilder();
            for (var i = 0; i < naive.Length; i++)
            {
                if (i > 0 && char.IsUpper(naive[i]))
                {
                    result.Append(' ');
                }

                result.Append(naive[i]);
            }

            return result.ToString();
        }
    }
}