package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetGrade: Double = 9.5,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "evaluations",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Evaluation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String,
    val grade: Double?, // null means evaluation pending, 0.0 to 20.0 is evaluated
    val weight: Double, // 0.0 to 100.0
    val category: String = "Otro" // "Parcial", "Quiz", "Exposición", "Laboratorio", "Taller", "Proyecto", "Otro"
)

data class SubjectWithEvaluations(
    @Embedded val subject: Subject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val evaluations: List<Evaluation>
) {
    // Total weight/percentage added by the student
    val totalWeight: Double 
        get() = evaluations.sumOf { it.weight }
    
    // Weight of completed/evaluated items (non-null grades)
    val evaluatedWeight: Double 
        get() = evaluations.filter { it.grade != null }.sumOf { it.weight }
    
    // Remaining weight (out of 100%)
    val remainingWeight: Double 
        get() = (100.0 - evaluatedWeight).coerceAtLeast(0.0)
    
    // Accumulated points out of 20.0 (G_accumulated = sum(grade * (weight/100)))
    val accumulatedPoints: Double 
        get() = evaluations.filter { it.grade != null }
            .sumOf { (it.grade ?: 0.0) * (it.weight / 100.0) }
        
    // Current weighted average (scaled to base 20 for completed items)
    val currentAverageGrade: Double 
        get() {
            val completed = evaluatedWeight
            if (completed <= 0.0) return 0.0
            return (accumulatedPoints / completed) * 100.0
        }
    
    // Core of "Modo Salvación": calculates what grade is needed on the remaining percentage to hit 9.5
    val requiredGradeForPass: Double? 
        get() {
            val target = subject.targetGrade // Default 9.5
            val remWeight = remainingWeight
            if (remWeight <= 0.1) {
                // If practically everything is graded, no remaining evaluation can be calculated
                return null
            }
            val neededPoints = target - accumulatedPoints
            if (neededPoints <= 0.0) {
                return 0.0 // Student already reached target
            }
            // gradeNeeded / 100.0 * remWeight = neededPoints => gradeNeeded = neededPoints / remWeight * 100
            val gradeNeeded = (neededPoints / remWeight) * 100.0
            return gradeNeeded
        }
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Transaction
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllSubjectsWithEvaluations(): Flow<List<SubjectWithEvaluations>>

    @Transaction
    @Query("SELECT * FROM subjects WHERE isArchived = 1 ORDER BY id DESC")
    fun getArchivedSubjectsWithEvaluations(): Flow<List<SubjectWithEvaluations>>

    @Query("UPDATE subjects SET isArchived = 1 WHERE isArchived = 0")
    suspend fun archiveAllActive()

    @Transaction
    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectWithEvaluationsById(id: Int): Flow<SubjectWithEvaluations?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectById(id: Int): Flow<Subject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    // Evaluations
    @Query("SELECT * FROM evaluations WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getEvaluationsForSubject(subjectId: Int): Flow<List<Evaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: Evaluation): Long

    @Update
    suspend fun updateEvaluation(evaluation: Evaluation)

    @Delete
    suspend fun deleteEvaluation(evaluation: Evaluation)
}

@Database(entities = [Subject::class, Evaluation::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
}
