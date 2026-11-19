package com.example.domain

import com.example.data.ChallengeDayStatus
import com.example.data.ChallengeLogEntity
import com.example.data.DateUtils
import com.example.data.FinanceTransactionEntity
import com.example.data.FocusSessionEntity
import com.example.data.HabitLogEntity
import com.example.data.LearningLevel
import com.example.data.LearningTopicEntity
import com.example.data.MascotMood
import com.example.data.NutritionEntryEntity
import com.example.data.SavingsGoalEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.TransactionType
import com.example.data.UserProfileEntity
import com.example.data.WaterLogEntity
import java.util.Locale
import kotlin.math.roundToInt

// ---------------- PERSONAL MONEY MANAGER ENGINE ----------------

data class FinanceSummary(
    val initialBalance: Double,
    val totalIncome: Double,
    val totalExpenses: Double,
    val availableBalance: Double,
    val todayExpense: Double,
    val monthlyExpense: Double,
    val monthlyBudget: Double,
    val budgetRemaining: Double,
    val dailySpendLimit: Double,
    val totalSavedInGoals: Double,
    val totalSavingsTarget: Double,
    val expenseByCategory: List<Pair<String, Double>>,
    val alerts: List<String>
)

object FinanceEngine {
    val defaultExpenseCategories = listOf(
        "Food", "Travel", "Shopping", "Rent", "Bills", "Education", "Health", "Entertainment", "Custom"
    )
    val defaultIncomeCategories = listOf(
        "Salary", "Pocket Money", "Freelance", "Bonus", "Other Income"
    )

    fun summarize(
        profile: UserProfileEntity,
        transactions: List<FinanceTransactionEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        dateIso: String = DateUtils.todayIso()
    ): FinanceSummary {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }
        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }
        val availableBalance = profile.initialBalance + totalIncome - totalExpenses

        val todayExpense = transactions
            .filter { it.type == TransactionType.EXPENSE.name && it.dateIso == dateIso }
            .sumOf { it.amount }

        val monthPrefix = dateIso.take(7) // YYYY-MM
        val monthlyExpense = transactions
            .filter { it.type == TransactionType.EXPENSE.name && it.dateIso.startsWith(monthPrefix) }
            .sumOf { it.amount }

        val budgetRemaining = profile.monthlyBudget - monthlyExpense
        val totalSaved = savingsGoals.sumOf { it.savedAmount }
        val totalTarget = savingsGoals.sumOf { it.targetAmount }

        val byCategory = transactions
            .filter { it.type == TransactionType.EXPENSE.name && it.dateIso.startsWith(monthPrefix) }
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        val alerts = mutableListOf<String>()
        if (profile.dailySpendLimit > 0 && todayExpense > profile.dailySpendLimit) {
            alerts += "Today's spending (${formatInr(todayExpense)}) exceeded your daily limit (${formatInr(profile.dailySpendLimit)})."
        }
        if (profile.monthlyBudget > 0 && monthlyExpense > profile.monthlyBudget) {
            alerts += "Monthly expenses (${formatInr(monthlyExpense)}) exceeded your budget (${formatInr(profile.monthlyBudget)})."
        } else if (profile.monthlyBudget > 0 && monthlyExpense >= profile.monthlyBudget * 0.85) {
            alerts += "You have used ${((monthlyExpense / profile.monthlyBudget) * 100).roundToInt()}% of your monthly budget."
        }
        if (availableBalance < 0) {
            alerts += "Your current available balance is negative (${formatInr(availableBalance)})."
        }

        return FinanceSummary(
            initialBalance = profile.initialBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            availableBalance = availableBalance,
            todayExpense = todayExpense,
            monthlyExpense = monthlyExpense,
            monthlyBudget = profile.monthlyBudget,
            budgetRemaining = budgetRemaining,
            dailySpendLimit = profile.dailySpendLimit,
            totalSavedInGoals = totalSaved,
            totalSavingsTarget = totalTarget,
            expenseByCategory = byCategory,
            alerts = alerts
        )
    }

    fun formatInr(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absVal = kotlin.math.abs(amount)
        return if (absVal == absVal.toLong().toDouble()) {
            "${sign}₹%,d".format(Locale.US, absVal.toLong())
        } else {
            "${sign}₹%,.2f".format(Locale.US, absVal)
        }
    }

    fun exportCsv(transactions: List<FinanceTransactionEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("date,type,category,amount_inr,note,location")
        transactions.forEach { tx ->
            fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""
            sb.appendLine("${tx.dateIso},${tx.type},${esc(tx.category)},${"%.2f".format(Locale.US, tx.amount)},${esc(tx.note)},${esc(tx.locationTag)}")
        }
        return sb.toString()
    }
}

// ---------------- NUTRITION SUMMARY ENGINE ----------------

data class DailyNutritionSummary(
    val dateIso: String,
    val totalProteinG: Double,
    val totalCalories: Int,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val proteinTargetG: Int,
    val calorieTargetKcal: Int,
    val entriesCount: Int
) {
    val proteinProgressFraction: Float
        get() = if (proteinTargetG <= 0) 0f else (totalProteinG / proteinTargetG).toFloat().coerceIn(0f, 1f)
    val calorieProgressFraction: Float
        get() = if (calorieTargetKcal <= 0) 0f else (totalCalories.toFloat() / calorieTargetKcal).coerceIn(0f, 1f)
}

data class EstimatedFoodPreset(
    val name: String,
    val quantityLabel: String,
    val proteinG: Double,
    val calories: Int,
    val carbsG: Double,
    val fatG: Double
)

object NutritionEngine {
    // Clearly labeled as general USDA/ICMR reference estimates (never claimed as lab-verified for a specific brand)
    val commonEstimatedFoods: List<EstimatedFoodPreset> = listOf(
        EstimatedFoodPreset("Boiled Eggs (2 large)", "2 eggs", 12.6, 155, 1.1, 10.6),
        EstimatedFoodPreset("Grilled Chicken Breast", "150 g", 46.0, 248, 0.0, 5.4),
        EstimatedFoodPreset("Paneer (Cottage Cheese)", "100 g", 18.0, 265, 3.0, 20.8),
        EstimatedFoodPreset("Cooked Toor / Moong Dal", "1 bowl (200 g)", 14.0, 220, 38.0, 1.5),
        EstimatedFoodPreset("Whey Protein Scoop", "1 scoop (30 g)", 24.0, 120, 3.0, 1.5),
        EstimatedFoodPreset("Greek Yogurt / Curd", "150 g", 15.0, 110, 6.0, 3.0),
        EstimatedFoodPreset("Rolled Oats with Milk", "1 bowl", 11.0, 290, 48.0, 6.0),
        EstimatedFoodPreset("Cooked Rice", "1 cup (160 g)", 4.3, 205, 45.0, 0.4),
        EstimatedFoodPreset("Chapati / Roti (2 pcs)", "2 medium", 6.5, 210, 42.0, 2.0),
        EstimatedFoodPreset("Roasted Peanuts / Almonds", "30 g handful", 7.0, 175, 6.0, 15.0),
        EstimatedFoodPreset("Banana (1 medium)", "1 fruit", 1.3, 105, 27.0, 0.4),
        EstimatedFoodPreset("Sprouts Salad", "1 bowl (100 g)", 9.0, 115, 19.0, 0.8)
    )

    fun summarizeForDate(
        entries: List<NutritionEntryEntity>,
        dateIso: String,
        proteinTargetG: Int,
        calorieTargetKcal: Int
    ): DailyNutritionSummary {
        val day = entries.filter { it.dateIso == dateIso }
        return DailyNutritionSummary(
            dateIso = dateIso,
            totalProteinG = day.sumOf { it.proteinG },
            totalCalories = day.sumOf { it.calories },
            totalCarbsG = day.sumOf { it.carbsG },
            totalFatG = day.sumOf { it.fatG },
            proteinTargetG = proteinTargetG,
            calorieTargetKcal = calorieTargetKcal,
            entriesCount = day.size
        )
    }
}

// ---------------- DUOLINGO-STYLE STREAK, XP & REACTIVE MASCOT ENGINE ----------------

data class StreakReport(
    val currentStreak: Int,
    val longestStreak: Int,
    val qualifiedToday: Boolean,
    val totalXp: Int,
    val level: Int,
    val xpInCurrentLevel: Int,
    val xpForNextLevel: Int,
    val weeklyStrip: List<Pair<String, Boolean>>, // shortDay -> active
    val monthlyHeatmap: List<Pair<String, Int>>,  // dateIso -> intensity 0..3
    val mascotMood: MascotMood,
    val mascotStatusLine: String,
    val activeMilestones: List<String>
)

object StreakAndXpEngine {

    fun calculate(
        todayIso: String,
        todayScorePercent: Int?,
        tasks: List<TaskEntity>,
        habitLogs: List<HabitLogEntity>,
        challengeLogs: List<ChallengeLogEntity>,
        focusSessions: List<FocusSessionEntity>,
        waterLogs: List<WaterLogEntity>,
        nutritionEntries: List<NutritionEntryEntity>,
        learningTopics: List<LearningTopicEntity>
    ): StreakReport {
        val activityCountByDay = mutableMapOf<String, Int>()
        fun bump(iso: String, weight: Int = 1) {
            if (iso.length == 10) {
                activityCountByDay[iso] = (activityCountByDay[iso] ?: 0) + weight
            }
        }

        tasks.filter { it.status == TaskStatus.COMPLETED.name && it.completedDateIso.isNotBlank() }
            .forEach { bump(it.completedDateIso, 1) }
        habitLogs.filter { it.completed }.forEach { bump(it.dateIso, 1) }
        challengeLogs.filter { it.status == ChallengeDayStatus.SUCCESS.name }.forEach { bump(it.dateIso, 1) }
        focusSessions.filter { it.actualSeconds >= 60 }.forEach { bump(it.dateIso, 1) }
        waterLogs.groupBy { it.dateIso }.forEach { (iso, logs) ->
            if (logs.sumOf { it.amountMl } >= 500) bump(iso, 1)
        }
        nutritionEntries.groupBy { it.dateIso }.forEach { (iso, _) -> bump(iso, 1) }

        val activeDates = activityCountByDay.keys
        val qualifiedToday = todayIso in activeDates

        // Current streak (counts today if active, or up to yesterday if today not yet finished)
        var currentStreak = 0
        var cursor = if (qualifiedToday) todayIso else DateUtils.offsetDateIso(todayIso, -1)
        while (cursor in activeDates && currentStreak < 3650) {
            currentStreak++
            cursor = DateUtils.offsetDateIso(cursor, -1)
        }
        // If neither today nor yesterday was active, streak is 0
        if (!qualifiedToday && DateUtils.offsetDateIso(todayIso, -1) !in activeDates) {
            currentStreak = 0
        }

        // Longest streak across all recorded dates
        val sortedDates = activeDates.sorted()
        var longest = 0
        var run = 0
        var prev: String? = null
        for (iso in sortedDates) {
            run = if (prev != null && DateUtils.offsetDateIso(prev, 1) == iso) run + 1 else 1
            if (run > longest) longest = run
            prev = iso
        }

        // XP Calculation (real earned XP only)
        val taskXp = tasks.count { it.status == TaskStatus.COMPLETED.name } * 15
        val habitXp = habitLogs.count { it.completed } * 12
        val challengeXp = challengeLogs.count { it.status == ChallengeDayStatus.SUCCESS.name } * 20
        val focusXp = focusSessions.sumOf { it.actualSeconds / 60 } * 2
        val learningXp = learningTopics.count { it.completed } * 25
        val waterXp = waterLogs.groupBy { it.dateIso }.count { it.value.sumOf { l -> l.amountMl } >= 1500 } * 10
        val nutritionXp = nutritionEntries.groupBy { it.dateIso }.size * 10
        val totalXp = taskXp + habitXp + challengeXp + focusXp + learningXp + waterXp + nutritionXp

        val xpPerLevel = 250
        val level = (totalXp / xpPerLevel) + 1
        val xpInLevel = totalXp % xpPerLevel

        val weeklyStrip = DateUtils.lastNDaysIso(7, todayIso).map { iso ->
            DateUtils.formatShortDay(iso) to (iso in activeDates)
        }
        val monthlyHeatmap = DateUtils.lastNDaysIso(28, todayIso).map { iso ->
            val cnt = activityCountByDay[iso] ?: 0
            val levelIntensity = when {
                cnt == 0 -> 0
                cnt == 1 -> 1
                cnt <= 3 -> 2
                else -> 3
            }
            iso to levelIntensity
        }

        val pendingTodayTasks = tasks.count { it.dueDateIso == todayIso && it.status != TaskStatus.COMPLETED.name }
        val overdueTasks = tasks.count { it.dueDateIso < todayIso && it.status != TaskStatus.COMPLETED.name }

        val mood: MascotMood
        val statusLine: String
        when {
            (todayScorePercent != null && todayScorePercent >= 70) || (qualifiedToday && currentStreak >= 3) -> {
                mood = MascotMood.HAPPY
                statusLine = if (currentStreak >= 3) {
                    "Awesome momentum! ${currentStreak}-day streak active."
                } else {
                    "Great work today — you're cruising through your goals!"
                }
            }
            qualifiedToday || (todayScorePercent != null && todayScorePercent >= 30) -> {
                mood = MascotMood.NEUTRAL
                statusLine = "Steady progress today. One more small win keeps the flow going!"
            }
            overdueTasks > 0 || (pendingTodayTasks > 0 && !qualifiedToday) -> {
                mood = MascotMood.SAD
                statusLine = "A fresh start is just one small step away — let's log one quick win."
            }
            else -> {
                mood = MascotMood.NEUTRAL
                statusLine = "Ready when you are. Log a task, habit, or glass of water to start your streak."
            }
        }

        val milestones = buildList {
            if (currentStreak >= 1) add("Day 1 Ignition")
            if (currentStreak >= 3) add("3-Day Momentum")
            if (currentStreak >= 7) add("7-Day Consistency")
            if (currentStreak >= 14) add("14-Day Builder")
            if (currentStreak >= 30) add("30-Day Mastery")
            if (learningTopics.count { it.completed } >= 5) add("Tech Learner")
            if (totalXp >= 500) add("500 XP Club")
        }

        return StreakReport(
            currentStreak = currentStreak,
            longestStreak = maxOf(longest, currentStreak),
            qualifiedToday = qualifiedToday,
            totalXp = totalXp,
            level = level,
            xpInCurrentLevel = xpInLevel,
            xpForNextLevel = xpPerLevel,
            weeklyStrip = weeklyStrip,
            monthlyHeatmap = monthlyHeatmap,
            mascotMood = mood,
            mascotStatusLine = statusLine,
            activeMilestones = milestones
        )
    }
}

// ---------------- TECH LEARNING HUB CATALOG (ALL 20 CATEGORIES) ----------------

data class TechTopicSeed(
    val id: String,
    val category: String,
    val level: LearningLevel,
    val title: String,
    val summary: String,
    val officialDocUrl: String
)

object TechLearningCatalog {
    val allCategories: List<String> = listOf(
        "Artificial Intelligence",
        "Machine Learning",
        "Java",
        "Advanced Java",
        "Python",
        "HTML",
        "CSS",
        "JavaScript",
        "React",
        "Angular",
        "Node.js",
        "MySQL",
        "MongoDB",
        "SQL",
        "Hibernate",
        "Spring Framework",
        "Spring Boot",
        "Data Structures & Algorithms",
        "Git & GitHub",
        "Java Full Stack Development"
    )

    val catalog: List<TechTopicSeed> = listOf(
        // Artificial Intelligence
        TechTopicSeed("ai_b1", "Artificial Intelligence", LearningLevel.BEGINNER, "Foundations of AI & Intelligent Agents", "Search strategies, heuristics, knowledge representation, and modern AI capabilities.", "https://ai.google.dev/docs"),
        TechTopicSeed("ai_i1", "Artificial Intelligence", LearningLevel.INTERMEDIATE, "Prompt Engineering & LLM Architecture", "Transformers, attention, tokenization, structured outputs, and context windows.", "https://ai.google.dev/gemini-api/docs"),
        TechTopicSeed("ai_a1", "Artificial Intelligence", LearningLevel.ADVANCED, "RAG, Tool Calling & Multi-Agent Systems", "Embeddings, vector retrieval, function calling, and evaluation harnesses.", "https://ai.google.dev/gemini-api/docs/function-calling"),

        // Machine Learning
        TechTopicSeed("ml_b1", "Machine Learning", LearningLevel.BEGINNER, "Supervised vs Unsupervised Learning", "Linear/logistic regression, train/val/test splits, and bias-variance tradeoff.", "https://developers.google.com/machine-learning/crash-course"),
        TechTopicSeed("ml_i1", "Machine Learning", LearningLevel.INTERMEDIATE, "Trees, Ensembles & Feature Engineering", "Decision trees, Random Forests, XGBoost, scaling, and cross-validation.", "https://scikit-learn.org/stable/user_guide.html"),
        TechTopicSeed("ml_a1", "Machine Learning", LearningLevel.ADVANCED, "Deep Neural Networks & MLOps", "Backpropagation, CNNs/RNNs, regularization, and production model monitoring.", "https://pytorch.org/tutorials/"),

        // Java
        TechTopicSeed("java_b1", "Java", LearningLevel.BEGINNER, "Core Java, OOP & Control Flow", "Classes, inheritance, polymorphism, interfaces, and exception handling.", "https://dev.java/learn/"),
        TechTopicSeed("java_i1", "Java", LearningLevel.INTERMEDIATE, "Collections Framework & Generics", "List, Set, Map internals, Comparator, HashMap hashing, and type bounds.", "https://dev.java/learn/api/collections-framework/"),
        TechTopicSeed("java_a1", "Java", LearningLevel.ADVANCED, "Streams, Lambdas & JVM Memory Model", "Functional interfaces, parallel streams, GC tuning, and records/sealed classes.", "https://dev.java/learn/lambdas/"),

        // Advanced Java
        TechTopicSeed("advjava_b1", "Advanced Java", LearningLevel.BEGINNER, "JDBC & Connection Pooling", "PreparedStatement, transactions, SQL injection prevention, and HikariCP.", "https://docs.oracle.com/javase/tutorial/jdbc/"),
        TechTopicSeed("advjava_i1", "Advanced Java", LearningLevel.INTERMEDIATE, "Servlets, JSP & HTTP Lifecycle", "Request/Response lifecycle, filters, sessions, and MVC web foundations.", "https://jakarta.ee/specifications/servlet/"),
        TechTopicSeed("advjava_a1", "Advanced Java", LearningLevel.ADVANCED, "Java Concurrency & Virtual Threads", "ExecutorService, CompletableFuture, locks, atomic classes, and Project Loom.", "https://dev.java/learn/jvm/"),

        // Python
        TechTopicSeed("py_b1", "Python", LearningLevel.BEGINNER, "Python Syntax, Data Types & Functions", "Lists, dicts, comprehensions, virtual environments, and modules.", "https://docs.python.org/3/tutorial/"),
        TechTopicSeed("py_i1", "Python", LearningLevel.INTERMEDIATE, "OOP, Decorators & Generators", "Context managers, iterators, type hints, and pytest unit testing.", "https://docs.python.org/3/library/"),
        TechTopicSeed("py_a1", "Python", LearningLevel.ADVANCED, "AsyncIO, FastAPI & Data Pipelines", "Coroutines, event loops, Pydantic validation, and high-throughput APIs.", "https://fastapi.tiangolo.com/"),

        // HTML
        TechTopicSeed("html_b1", "HTML", LearningLevel.BEGINNER, "Semantic HTML5 & Document Structure", "Header, main, nav, article, section, and clean heading hierarchy.", "https://developer.mozilla.org/en-US/docs/Web/HTML"),
        TechTopicSeed("html_i1", "HTML", LearningLevel.INTERMEDIATE, "Forms, Validation & Media Elements", "Accessible labels, input types, picture/srcset, and native form constraints.", "https://developer.mozilla.org/en-US/docs/Learn/Forms"),
        TechTopicSeed("html_a1", "HTML", LearningLevel.ADVANCED, "ARIA Accessibility & SEO Metadata", "Landmarks, keyboard focus management, Open Graph, and structured data.", "https://developer.mozilla.org/en-US/docs/Web/Accessibility"),

        // CSS
        TechTopicSeed("css_b1", "CSS", LearningLevel.BEGINNER, "Box Model, Selectors & Flexbox", "Specificity, cascade, spacing, and 1D responsive layouts with Flexbox.", "https://developer.mozilla.org/en-US/docs/Web/CSS"),
        TechTopicSeed("css_i1", "CSS", LearningLevel.INTERMEDIATE, "CSS Grid & Responsive Design", "Grid template areas, clamp(), container queries, and custom properties.", "https://web.dev/learn/css/"),
        TechTopicSeed("css_a1", "CSS", LearningLevel.ADVANCED, "Animations, Glassmorphism & Performance", "Compositor-friendly transforms, backdrop-filter, and design token systems.", "https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_animations"),

        // JavaScript
        TechTopicSeed("js_b1", "JavaScript", LearningLevel.BEGINNER, "ES6+ Fundamentals & DOM Manipulation", "let/const, arrow functions, destructuring, modules, and events.", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide"),
        TechTopicSeed("js_i1", "JavaScript", LearningLevel.INTERMEDIATE, "Closures, Prototypes & Async/Await", "Promises, Fetch API, error handling, and lexical scope.", "https://javascript.info/"),
        TechTopicSeed("js_a1", "JavaScript", LearningLevel.ADVANCED, "Event Loop, Performance & TypeScript", "Microtasks vs macrotasks, memory profiling, and strict static typing.", "https://www.typescriptlang.org/docs/"),

        // React
        TechTopicSeed("react_b1", "React", LearningLevel.BEGINNER, "Components, Props & State (useState)", "JSX, declarative rendering, lists, keys, and controlled forms.", "https://react.dev/learn"),
        TechTopicSeed("react_i1", "React", LearningLevel.INTERMEDIATE, "Hooks, Context & Routing", "useEffect hygiene, custom hooks, useReducer, and client navigation.", "https://react.dev/reference/react/hooks"),
        TechTopicSeed("react_a1", "React", LearningLevel.ADVANCED, "Performance, Server Components & Testing", "Memoization, suspense boundaries, query caching, and testing library.", "https://react.dev/learn/render-and-commit"),

        // Angular
        TechTopicSeed("ng_b1", "Angular", LearningLevel.BEGINNER, "Standalone Components & Templates", "Signals, control flow (@if/@for), data binding, and CLI basics.", "https://angular.dev/overview"),
        TechTopicSeed("ng_i1", "Angular", LearningLevel.INTERMEDIATE, "Services, Dependency Injection & RxJS", "Observables, HttpClient, reactive forms, and route guards.", "https://angular.dev/guide/di"),
        TechTopicSeed("ng_a1", "Angular", LearningLevel.ADVANCED, "Change Detection, NgRx & Optimization", "Zoneless Angular, deferrable views, and enterprise state architecture.", "https://angular.dev/best-practices/runtime-performance"),

        // Node.js
        TechTopicSeed("node_b1", "Node.js", LearningLevel.BEGINNER, "Node Runtime, npm & File System", "CommonJS vs ESM, streams, buffers, and environment configuration.", "https://nodejs.org/en/learn/getting-started/introduction-to-nodejs"),
        TechTopicSeed("node_i1", "Node.js", LearningLevel.INTERMEDIATE, "Express REST APIs & Middleware", "Routing, validation, JWT authentication, and centralized error handlers.", "https://expressjs.com/en/guide/routing.html"),
        TechTopicSeed("node_a1", "Node.js", LearningLevel.ADVANCED, "Scaling, Worker Threads & Security", "Rate limiting, clustering, logging, and production observability.", "https://nodejs.org/api/worker_threads.html"),

        // MySQL
        TechTopicSeed("mysql_b1", "MySQL", LearningLevel.BEGINNER, "Tables, Keys & CRUD Operations", "Primary/foreign keys, data types, constraints, and schema creation.", "https://dev.mysql.com/doc/refman/8.4/en/tutorial.html"),
        TechTopicSeed("mysql_i1", "MySQL", LearningLevel.INTERMEDIATE, "Indexes, Normalization & Joins", "B-Tree indexes, 1NF–3NF normalization, views, and stored routines.", "https://dev.mysql.com/doc/refman/8.4/en/optimization-indexes.html"),
        TechTopicSeed("mysql_a1", "MySQL", LearningLevel.ADVANCED, "Transactions, ACID & EXPLAIN Tuning", "InnoDB locking, isolation levels, query plans, and replication.", "https://dev.mysql.com/doc/refman/8.4/en/explain-output.html"),

        // MongoDB
        TechTopicSeed("mongo_b1", "MongoDB", LearningLevel.BEGINNER, "Document Model & CRUD Queries", "BSON documents, collections, insert/find/update/delete operators.", "https://www.mongodb.com/docs/manual/crud/"),
        TechTopicSeed("mongo_i1", "MongoDB", LearningLevel.INTERMEDIATE, "Aggregation Pipeline & Schema Patterns", "Match, group, lookup, embedding vs referencing, and compound indexes.", "https://www.mongodb.com/docs/manual/aggregation/"),
        TechTopicSeed("mongo_a1", "MongoDB", LearningLevel.ADVANCED, "Sharding, Replica Sets & Atlas Search", "High availability, read/write concerns, and performance profiling.", "https://www.mongodb.com/docs/manual/replication/"),

        // SQL
        TechTopicSeed("sql_b1", "SQL", LearningLevel.BEGINNER, "SELECT, WHERE, GROUP BY & Aggregates", "Filtering, sorting, COUNT/SUM/AVG, and HAVING clauses.", "https://www.postgresql.org/docs/current/tutorial-sql.html"),
        TechTopicSeed("sql_i1", "SQL", LearningLevel.INTERMEDIATE, "INNER/LEFT/FULL Joins & Subqueries", "Multi-table relational queries, UNION, and correlated subqueries.", "https://www.postgresql.org/docs/current/queries-table-expressions.html"),
        TechTopicSeed("sql_a1", "SQL", LearningLevel.ADVANCED, "Window Functions & CTEs", "ROW_NUMBER, RANK, LAG/LEAD, recursive CTEs, and query optimization.", "https://www.postgresql.org/docs/current/tutorial-window.html"),

        // Hibernate
        TechTopicSeed("hib_b1", "Hibernate", LearningLevel.BEGINNER, "ORM Mapping & JPA Entities", "@Entity, @Id generation strategies, SessionFactory, and EntityManager.", "https://hibernate.org/orm/documentation/"),
        TechTopicSeed("hib_i1", "Hibernate", LearningLevel.INTERMEDIATE, "Relationships & Fetch Strategies", "@OneToMany, @ManyToOne, Lazy vs Eager loading, and cascading.", "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html"),
        TechTopicSeed("hib_a1", "Hibernate", LearningLevel.ADVANCED, "N+1 Prevention, Caching & Locking", "JOIN FETCH, EntityGraph, 2nd-level cache, and optimistic locking.", "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#caching"),

        // Spring Framework
        TechTopicSeed("spring_b1", "Spring Framework", LearningLevel.BEGINNER, "IoC Container & Dependency Injection", "Beans, @Component/@Service/@Repository, and constructor injection.", "https://docs.spring.io/spring-framework/reference/core.html"),
        TechTopicSeed("spring_i1", "Spring Framework", LearningLevel.INTERMEDIATE, "Spring MVC, AOP & Transactions", "DispatcherServlet, @Transactional boundaries, and aspect-oriented logging.", "https://docs.spring.io/spring-framework/reference/web/webmvc.html"),
        TechTopicSeed("spring_a1", "Spring Framework", LearningLevel.ADVANCED, "Spring Security & OAuth2/JWT", "SecurityFilterChain, RBAC, password hashing, and stateless JWT auth.", "https://docs.spring.io/spring-security/reference/"),

        // Spring Boot
        TechTopicSeed("sb_b1", "Spring Boot", LearningLevel.BEGINNER, "Auto-Configuration & REST Controllers", "Starters, @RestController, DTO validation, and application.yml.", "https://docs.spring.io/spring-boot/index.html"),
        TechTopicSeed("sb_i1", "Spring Boot", LearningLevel.INTERMEDIATE, "Spring Data JPA & Global Exception Handling", "Repositories, pagination, @ControllerAdvice, and Testcontainers.", "https://docs.spring.io/spring-data/jpa/reference/"),
        TechTopicSeed("sb_a1", "Spring Boot", LearningLevel.ADVANCED, "Microservices, Actuator & Docker Deployment", "Health probes, OpenAPI docs, distributed tracing, and containerization.", "https://docs.spring.io/spring-boot/reference/actuator/index.html"),

        // Data Structures & Algorithms
        TechTopicSeed("dsa_b1", "Data Structures & Algorithms", LearningLevel.BEGINNER, "Big-O, Arrays, Strings & Linked Lists", "Time/space complexity, two pointers, sliding window, and hashing.", "https://cp-algorithms.com/"),
        TechTopicSeed("dsa_i1", "Data Structures & Algorithms", LearningLevel.INTERMEDIATE, "Stacks, Queues, Trees & Binary Search", "BFS/DFS traversals, BSTs, heaps/priority queues, and recursion.", "https://visualgo.net/en"),
        TechTopicSeed("dsa_a1", "Data Structures & Algorithms", LearningLevel.ADVANCED, "Graphs, Dynamic Programming & Greedy", "Dijkstra, topological sort, union-find, memoization, and tabulation.", "https://cp-algorithms.com/graph/breadth-first-search.html"),

        // Git & GitHub
        TechTopicSeed("git_b1", "Git & GitHub", LearningLevel.BEGINNER, "Init, Staging, Commits & Branching", "Working tree, status, diff, checkout/switch, and clean commit messages.", "https://git-scm.com/book/en/v2"),
        TechTopicSeed("git_i1", "Git & GitHub", LearningLevel.INTERMEDIATE, "Pull Requests, Merging & Conflict Resolution", "Forks, code reviews, resolving merge conflicts, and stashing.", "https://docs.github.com/en/get-started"),
        TechTopicSeed("git_a1", "Git & GitHub", LearningLevel.ADVANCED, "Interactive Rebase, Cherry-Pick & GitHub Actions", "Clean history workflows, bisect debugging, and CI/CD pipelines.", "https://docs.github.com/en/actions"),

        // Java Full Stack Development
        TechTopicSeed("jfs_b1", "Java Full Stack Development", LearningLevel.BEGINNER, "Full-Stack Architecture Blueprint", "Connecting React/Angular frontend with Spring Boot REST APIs and MySQL.", "https://spring.io/guides"),
        TechTopicSeed("jfs_i1", "Java Full Stack Development", LearningLevel.INTERMEDIATE, "End-to-End Auth, Validation & State Sync", "CORS, JWT cookies/headers, pagination, and shared API contracts.", "https://spring.io/guides/tutorials/react-and-spring-data-rest"),
        TechTopicSeed("jfs_a1", "Java Full Stack Development", LearningLevel.ADVANCED, "Cloud Deployment, CI/CD & Capstone Portfolio", "Docker Compose, automated testing, observability, and interview walkthroughs.", "https://docs.docker.com/get-started/")
    )

    fun seedForUser(uid: String): List<LearningTopicEntity> = catalog.map { seed ->
        LearningTopicEntity(
            id = "${uid}_${seed.id}",
            userId = uid,
            category = seed.category,
            level = seed.level.name,
            title = seed.title
        )
    }

    fun metadataFor(title: String): TechTopicSeed? = catalog.firstOrNull { it.title == title }
}
