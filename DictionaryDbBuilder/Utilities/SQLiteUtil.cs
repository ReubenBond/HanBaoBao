namespace DictionaryDbBuilder.Utilities
{
    using System;
    using System.Data.SQLite;

    public static class SqliteUtil
    {
        public static void AddIfSet(
            this SQLiteParameterCollection parameters, 
            string paramName, 
            string[] tokens, 
            int index, 
            Func<string, string> modifier = null)
        {
            if (index >= tokens.Length)
            {
                return;
            }

            if (!string.IsNullOrWhiteSpace(tokens[index]) && tokens[index] != "\\N")
            {
                var value = tokens[index];
                if (modifier != null)
                {
                    value = modifier(value);
                }

                if (string.IsNullOrWhiteSpace(value))
                {
                    return;
                }

                parameters.AddWithValue(paramName, value);
            }
        }

        public static void AddIfSet(
            this SQLiteParameterCollection parameters, 
            string paramName, 
            string value, 
            Func<string, string> modifier = null)
        {
            if (modifier != null)
            {
                value = modifier(value);
            }

            parameters.AddWithValue(paramName, value);
        }
    }
}