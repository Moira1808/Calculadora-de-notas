package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Evaluation
import com.example.data.Subject
import com.example.data.SubjectRepository
import com.example.data.SubjectWithEvaluations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SubjectViewModel(private val repository: SubjectRepository) : ViewModel() {

    // Main Stream: All subjects with their evaluations
    val allSubjectsWithEvaluations: StateFlow<List<SubjectWithEvaluations>> = repository.allSubjectsWithEvaluations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedSubjectsWithEvaluations: StateFlow<List<SubjectWithEvaluations>> = repository.archivedSubjectsWithEvaluations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current screen navigation state: Null means Home (dashboard list), non-null is the details of a subject
    private val _selectedSubjectId = MutableStateFlow<Int?>(null)
    val selectedSubjectId: StateFlow<Int?> = _selectedSubjectId.asStateFlow()

    // Simulator Mode values
    private val _isSimulatorMode = MutableStateFlow(false)
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private val _simulatedEvaluations = MutableStateFlow<List<Evaluation>?>(null)
    val simulatedEvaluations: StateFlow<List<Evaluation>?> = _simulatedEvaluations.asStateFlow()

    // Real selected subject evaluations
    @OptIn(ExperimentalCoroutinesApi::class)
    private val realSelectedSubjectWithEvaluations: StateFlow<SubjectWithEvaluations?> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                repository.getSubjectWithEvaluationsById(id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Active detail item stream: dynamically pipes simulated copy if Simulator Mode is active!
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedSubjectWithEvaluations: StateFlow<SubjectWithEvaluations?> = combine(
        realSelectedSubjectWithEvaluations,
        _isSimulatorMode,
        _simulatedEvaluations
    ) { realSubject, isSim, simEvals ->
        if (realSubject == null) return@combine null
        if (isSim && simEvals != null) {
            realSubject.copy(evaluations = simEvals)
        } else {
            realSubject
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectSubject(subjectId: Int?) {
        _selectedSubjectId.value = subjectId
        // Turn off simulator mode when switching subjects
        toggleSimulatorMode(false)
    }

    fun toggleSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        if (enabled) {
            val currentReal = realSelectedSubjectWithEvaluations.value
            if (currentReal != null) {
                _simulatedEvaluations.value = currentReal.evaluations.map { it.copy() }
            } else {
                _simulatedEvaluations.value = emptyList()
            }
        } else {
            _simulatedEvaluations.value = null
        }
    }

    // --- SUBJECT CRUD ---
    fun addSubject(name: String, targetGrade: Double = 9.5) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val subject = Subject(name = name.trim(), targetGrade = targetGrade)
                val newId = repository.insertSubject(subject)
                // Optionally open the newly created subject immediately
                _selectedSubjectId.value = newId.toInt()
            }
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            // If the deleted subject is currently selected, go back to home first
            if (_selectedSubjectId.value == subject.id) {
                _selectedSubjectId.value = null
            }
            repository.deleteSubject(subject)
        }
    }

    // --- EVALUATION CRUD ---
    fun addEvaluation(subjectId: Int, name: String, grade: Double?, weight: Double, category: String = "Otro") {
        if (_isSimulatorMode.value) {
            val currentList = _simulatedEvaluations.value?.toMutableList() ?: mutableListOf()
            val tempId = -(currentList.size + 1)
            val eval = Evaluation(
                id = tempId,
                subjectId = subjectId,
                name = name.trim().ifEmpty { "Evaluación" },
                grade = grade,
                weight = weight,
                category = category
            )
            currentList.add(eval)
            _simulatedEvaluations.value = currentList
        } else {
            viewModelScope.launch {
                val eval = Evaluation(
                    subjectId = subjectId,
                    name = name.trim().ifEmpty { "Evaluación" },
                    grade = grade,
                    weight = weight,
                    category = category
                )
                repository.insertEvaluation(eval)
            }
        }
    }

    fun updateEvaluation(evaluation: Evaluation) {
        if (_isSimulatorMode.value) {
            val currentList = _simulatedEvaluations.value?.map {
                if (it.id == evaluation.id) evaluation else it
            }
            _simulatedEvaluations.value = currentList
        } else {
            viewModelScope.launch {
                repository.updateEvaluation(evaluation)
            }
        }
    }

    fun deleteEvaluation(evaluation: Evaluation) {
        if (_isSimulatorMode.value) {
            val currentList = _simulatedEvaluations.value?.filter { it.id != evaluation.id }
            _simulatedEvaluations.value = currentList
        } else {
            viewModelScope.launch {
                repository.deleteEvaluation(evaluation)
            }
        }
    }

    // --- SYSTEM OPERATIONS ---
    fun archiveTrimester() {
        viewModelScope.launch {
            _selectedSubjectId.value = null
            repository.archiveAllActive()
        }
    }

    // The Panic / Reset button clears everything in the database
    fun resetTrimester() {
        viewModelScope.launch {
            _selectedSubjectId.value = null
            repository.deleteAllSubjects()
        }
    }
}

class SubjectViewModelFactory(private val repository: SubjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubjectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
