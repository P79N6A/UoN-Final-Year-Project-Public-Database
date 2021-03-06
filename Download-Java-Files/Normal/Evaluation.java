package nwoolcan.model.brewery.batch.review;

import java.util.Optional;

/**
 * Represent an evaluation.
 */
public interface Evaluation {
    /**
     * Returns the score.
     * @return the score.
     */
    int getScore();
    /**
     * Returns the type of this evaluation.
     * @return the maximum possible score.
     */
    EvaluationType getEvaluationType();
    /**
     * @return the notes for this evaluation, if any.
     */
    Optional<String> getNotes();
}
