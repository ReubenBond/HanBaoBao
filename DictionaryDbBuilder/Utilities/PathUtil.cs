namespace DictionaryDbBuilder.Utilities
{
    using System;
    using System.IO;

    public static class PathUtil
    {
        /// <summary>
        ///     Returns the current assembly path.
        /// </summary>
        /// <returns>
        ///     The current assembly path.
        /// </returns>
        public static string GetAssemblyPath(this Type type)
        {
            return Path.GetDirectoryName(new Uri(type.Assembly.CodeBase).LocalPath);
        }
    }
}