namespace DictionaryDbBuilder.Utilities.StartDict
{
    public interface IDictDb
    {
        byte[] GetBytes(long offset, int length);

        /// <summary>
        ///     Retrieves a entry from dictionary the database (plain txt or dictzip)
        /// </summary>
        /// <returns>
        ///     The entry string, encoding conversion should be done by the implementation
        /// </returns>
        /// <param name='offset'>
        ///     Offset. Currently rarely a dictionary would be bigger than 4GB,
        ///     yet make it long anyway for future extension.
        /// </param>
        /// <param name='length'>
        ///     Length.
        /// </param>
        string GetEntry(long offset, int length);
    }

    public interface IDictAddress
    {
        int Length { get; }

        long Offset { get; }
    }

    public interface IDictIdx
    {
        // use constraint to avoid boxing of value types
        bool GetAddress(int ordinal, out long offset, out int length);

        bool GetIndexRange(string headword, out int begin, out int end);
    }
}