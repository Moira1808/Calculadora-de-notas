package com.example.data

import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {

    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    val allSubjectsWithEvaluations: Flow<List<SubjectWithEvaluations>> = 
        subjectDao.getAllSubjectsWithEvaluations()

    val archivedSubjectsWithEvaluations: Flow<List<SubjectWithEvaluations>> = 
        subjectDao.getArchivedSubjectsWithEvaluations()

    fun getSubjectWithEvaluationsById(id: Int): Flow<SubjectWithEvaluations?> = 
        subjectDao.getSubjectWithEvaluationsById(id)

    fun getSubjectById(id: Int): Flow<Subject?> = subjectDao.getSubjectById(id)

    fun getEvaluationsForSubject(subjectId: Int): Flow<List<Evaluation>> = 
        subjectDao.getEvaluationsForSubject(subjectId)

    suspend fun insertSubject(subject: Subject): Long = 
        subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = 
        subjectDao.updateSubject(subject)

    suspend fun deleteSubject(subject: Subject) = 
        subjectDao.deleteSubject(subject)

    suspend fun archiveAllActive() = 
        subjectDao.archiveAllActive()

    suspend fun deleteAllSubjects() = 
        subjectDao.deleteAllSubjects()

    suspend fun insertEvaluation(evaluation: Evaluation): Long = 
        subjectDao.insertEvaluation(evaluation)

    suspend fun updateEvaluation(evaluation: Evaluation) = 
        subjectDao.updateEvaluation(evaluation)

    suspend fun deleteEvaluation(evaluation: Evaluation) = 
        subjectDao.deleteEvaluation(evaluation)
}
