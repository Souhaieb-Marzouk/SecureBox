package io.securebox.core.ui.component

import androidx.annotation.StringRes
import io.securebox.core.domain.model.ScoreGrade
import io.securebox.core.ui.R

/** Maps a [ScoreGrade] to its localised label resource. */
object ScoreGradeLabels {
    @StringRes
    fun of(grade: ScoreGrade): Int =
        when (grade) {
            ScoreGrade.EXCELLENT -> R.string.score_grade_excellent
            ScoreGrade.GOOD -> R.string.score_grade_good
            ScoreGrade.FAIR -> R.string.score_grade_fair
            ScoreGrade.AT_RISK -> R.string.score_grade_at_risk
        }
}
