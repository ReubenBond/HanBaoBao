namespace Common.Models
{
    using System;

    public class UserProfile
    {
        public Guid Id { get; set; }
        public int Points { get; set; }
        public string DisplayName { get; set; }
        public string Email { get; set; }
        public string DisplayPicture { get; set; }
        public DateTime Joined { get; set; }
        public DateTime LastActive { get; set; }
    }

    public class UserProfileUpdate
    {
        public string DisplayName { get; set; }
        public string Email { get; set; }
        public string DisplayPicture { get; set; }
    }
}