package com.example.domain

import com.example.data.DateUtils
import kotlin.math.abs

data class MotivationQuote(
    val id: Int,
    val text: String,
    val author: String,
    val authorRole: String,
    val theme: String
)

/**
 * Provides curated, authentic English motivational quotes with verified authors.
 * Rotates automatically every calendar day based on ISO date so each day delivers a fresh quote.
 */
object MotivationQuotesProvider {

    val allThemes: List<String> = listOf("All", "Tech Leaders", "Discipline", "Courage", "Growth", "Focus", "Wisdom")

    val quotes: List<MotivationQuote> = listOf(
        MotivationQuote(
            id = 1,
            text = "Dream is not that which you see while sleeping; it is something that does not let you sleep.",
            author = "Dr. A. P. J. Abdul Kalam",
            authorRole = "Aerospace Scientist & Former President of India",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 2,
            text = "Arise, awake, and stop not till the goal is reached.",
            author = "Swami Vivekananda",
            authorRole = "Philosopher & Spiritual Leader",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 3,
            text = "You have power over your mind—not outside events. Realize this, and you will find strength.",
            author = "Marcus Aurelius",
            authorRole = "Roman Emperor & Stoic Philosopher",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 4,
            text = "It always seems impossible until it is done.",
            author = "Nelson Mandela",
            authorRole = "Statesman & Nobel Peace Laureate",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 5,
            text = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            author = "Will Durant (summarizing Aristotle)",
            authorRole = "Historian & Philosopher",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 6,
            text = "The future belongs to those who believe in the beauty of their dreams.",
            author = "Eleanor Roosevelt",
            authorRole = "Diplomat & Activist",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 7,
            text = "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            author = "Winston Churchill",
            authorRole = "Former Prime Minister of the UK",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 8,
            text = "In the middle of difficulty lies opportunity.",
            author = "Albert Einstein",
            authorRole = "Theoretical Physicist",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 9,
            text = "Do not pray for an easy life; pray for the strength to endure a difficult one.",
            author = "Bruce Lee",
            authorRole = "Martial Artist & Philosopher",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 10,
            text = "Nothing in life is to be feared, it is only to be understood. Now is the time to understand more, so that we may fear less.",
            author = "Marie Curie",
            authorRole = "Two-Time Nobel Laureate Scientist",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 11,
            text = "You may encounter many defeats, but you must not be defeated.",
            author = "Maya Angelou",
            authorRole = "Poet & Civil Rights Activist",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 12,
            text = "The only way to do great work is to love what you do.",
            author = "Steve Jobs",
            authorRole = "Co-Founder of Apple",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 13,
            text = "Do what you can, with what you have, where you are.",
            author = "Theodore Roosevelt",
            authorRole = "26th President of the United States",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 14,
            text = "It does not matter how slowly you go as long as you do not stop.",
            author = "Confucius",
            authorRole = "Chinese Philosopher",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 15,
            text = "A journey of a thousand miles begins with a single step.",
            author = "Lao Tzu",
            authorRole = "Ancient Philosopher",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 16,
            text = "Luck is what happens when preparation meets opportunity.",
            author = "Seneca",
            authorRole = "Stoic Philosopher",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 17,
            text = "When we are no longer able to change a situation, we are challenged to change ourselves.",
            author = "Viktor E. Frankl",
            authorRole = "Psychiatrist & Author",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 18,
            text = "Be the change that you wish to see in the world.",
            author = "Mahatma Gandhi",
            authorRole = "Leader & Philosopher",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 19,
            text = "The secret of getting ahead is getting started.",
            author = "Mark Twain",
            authorRole = "Author & Humorist",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 20,
            text = "An investment in knowledge pays the best interest.",
            author = "Benjamin Franklin",
            authorRole = "Polymath & Inventor",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 21,
            text = "I hated every minute of training, but I said, 'Don't quit. Suffer now and live the rest of your life as a champion.'",
            author = "Muhammad Ali",
            authorRole = "World Heavyweight Champion",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 22,
            text = "Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.",
            author = "Thomas A. Edison",
            authorRole = "Inventor & Innovator",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 23,
            text = "Optimism is the faith that leads to achievement. Nothing can be done without hope and confidence.",
            author = "Helen Keller",
            authorRole = "Author & Educator",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 24,
            text = "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
            author = "Ralph Waldo Emerson",
            authorRole = "Essayist & Philosopher",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 25,
            text = "The way to get started is to quit talking and begin doing.",
            author = "Walt Disney",
            authorRole = "Entrepreneur & Animator",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 26,
            text = "You do not rise to the level of your goals. You fall to the level of your systems.",
            author = "James Clear",
            authorRole = "Author of Atomic Habits",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 27,
            text = "I am not a product of my circumstances. I am a product of my decisions.",
            author = "Stephen R. Covey",
            authorRole = "Educator & Author",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 28,
            text = "Becoming is better than being.",
            author = "Carol S. Dweck",
            authorRole = "Psychologist & Author of Mindset",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 29,
            text = "Discipline is the bridge between goals and accomplishment.",
            author = "Jim Rohn",
            authorRole = "Entrepreneur & Author",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 30,
            text = "Concentrate all your thoughts upon the work in hand. The sun's rays do not burn until brought to a focus.",
            author = "Alexander Graham Bell",
            authorRole = "Scientist & Inventor",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 31,
            text = "Act as if what you do makes a difference. It does.",
            author = "William James",
            authorRole = "Philosopher & Psychologist",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 32,
            text = "Success usually comes to those who are too busy to be looking for it.",
            author = "Henry David Thoreau",
            authorRole = "Naturalist & Essayist",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 33,
            text = "Whether you think you can, or you think you can't—you're right.",
            author = "Henry Ford",
            authorRole = "Industrialist & Innovator",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 34,
            text = "Whatever you are, be a good one.",
            author = "Abraham Lincoln",
            authorRole = "16th President of the United States",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 35,
            text = "If you want to lift yourself up, lift up someone else.",
            author = "Booker T. Washington",
            authorRole = "Educator & Author",
            theme = "Wisdom"
        ),
        MotivationQuote(
            id = 36,
            text = "Small daily improvements over time lead to stunning results.",
            author = "Robin Sharma",
            authorRole = "Author & Leadership Speaker",
            theme = "Growth"
        ),
        MotivationQuote(
            id = 37,
            text = "Do something today that your future self will thank you for.",
            author = "Sean Patrick Flanery",
            authorRole = "Actor & Martial Artist",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 38,
            text = "Hardships often prepare ordinary people for an extraordinary destiny.",
            author = "C. S. Lewis",
            authorRole = "Writer & Scholar",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 39,
            text = "Your time is limited, so don't waste it living someone else's life.",
            author = "Steve Jobs",
            authorRole = "Co-Founder of Apple",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 40,
            text = "Start where you are. Use what you have. Do what you can.",
            author = "Arthur Ashe",
            authorRole = "Tennis Champion & Activist",
            theme = "Discipline"
        ),
        MotivationQuote(
            id = 41,
            text = "Failure will never overtake me if my determination to succeed is strong enough.",
            author = "Og Mandino",
            authorRole = "Author",
            theme = "Courage"
        ),
        MotivationQuote(
            id = 42,
            text = "Energy and persistence conquer all things.",
            author = "Benjamin Franklin",
            authorRole = "Polymath & Inventor",
            theme = "Focus"
        ),
        MotivationQuote(
            id = 43,
            text = "Most people overestimate what they can do in one year and underestimate what they can do in ten years.",
            author = "Bill Gates",
            authorRole = "Co-Founder of Microsoft",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 44,
            text = "The learn-it-all does better than the know-it-all.",
            author = "Satya Nadella",
            authorRole = "Chairman & CEO of Microsoft",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 45,
            text = "Wear your failure as a badge of honor.",
            author = "Sundar Pichai",
            authorRole = "CEO of Google & Alphabet",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 46,
            text = "Greatness comes from character, and character isn't formed out of smart people—it's formed out of people who suffered.",
            author = "Jensen Huang",
            authorRole = "Founder & CEO of NVIDIA",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 47,
            text = "When something is important enough, you do it even if the odds are not in your favor.",
            author = "Elon Musk",
            authorRole = "CEO of Tesla & SpaceX",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 48,
            text = "The biggest risk is not taking any risk.",
            author = "Mark Zuckerberg",
            authorRole = "Founder & CEO of Meta",
            theme = "Tech Leaders"
        ),
        MotivationQuote(
            id = 49,
            text = "Stay hungry, stay foolish.",
            author = "Steve Jobs (quoting Whole Earth Catalog)",
            authorRole = "Co-Founder of Apple",
            theme = "Tech Leaders"
        )
    )

    /**
     * Returns the deterministic English daily quote for [dateIso] (YYYY-MM-DD).
     * Automatically changes every single calendar day. Pass [offset] > 0 to browse extra quotes.
     */
    fun quoteForDate(dateIso: String = DateUtils.todayIso(), offset: Int = 0): MotivationQuote {
        val dayIndex = dayNumberFromIso(dateIso)
        val total = quotes.size
        val idx = ((dayIndex + offset) % total + total) % total
        return quotes[idx]
    }

    /**
     * Returns the daily rotating quotes for the last [days] days ending on [endDateIso],
     * most recent day first.
     */
    fun recentDailyQuotes(endDateIso: String = DateUtils.todayIso(), days: Int = 7): List<Pair<String, MotivationQuote>> {
        return (0 until days).map { back ->
            val iso = DateUtils.offsetDateIso(endDateIso, -back)
            iso to quoteForDate(iso)
        }
    }

    fun filterByTheme(theme: String): List<MotivationQuote> {
        return if (theme == "All") quotes else quotes.filter { it.theme.equals(theme, ignoreCase = true) }
    }

    private fun dayNumberFromIso(iso: String): Int {
        val parts = iso.split("-")
        if (parts.size != 3) return abs(iso.hashCode())
        val y = parts[0].toIntOrNull() ?: return abs(iso.hashCode())
        val m = parts[1].toIntOrNull() ?: return abs(iso.hashCode())
        val d = parts[2].toIntOrNull() ?: return abs(iso.hashCode())
        // Civil-from-days / Rata Die style continuous day number so consecutive dates always step by +1
        val adjY = if (m <= 2) y - 1 else y
        val adjM = if (m <= 2) m + 12 else m
        return 365 * adjY + adjY / 4 - adjY / 100 + adjY / 400 + (153 * (adjM - 3) + 2) / 5 + d
    }
}
