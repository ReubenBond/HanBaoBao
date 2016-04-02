namespace DictionaryDbBuilder.Utilities
{
    using System.Text;

    public static class StringUtil
    {
        public static StringBuilder AppendSentenceCase(this StringBuilder builder, string input)
        {
            if (string.IsNullOrWhiteSpace(input))
            {
                return builder;
            }

            builder.Append(char.ToUpperInvariant(input[0]));
            for (var i = 1; i < input.Length; i++)
            {
                builder.Append(input[i]);
            }

            return builder;
        }

        public static string ToSentenceCase(this string input)
        {
            if (string.IsNullOrWhiteSpace(input))
            {
                return null;
            }

            return AppendSentenceCase(new StringBuilder(), input).ToString();
        }
    }
}