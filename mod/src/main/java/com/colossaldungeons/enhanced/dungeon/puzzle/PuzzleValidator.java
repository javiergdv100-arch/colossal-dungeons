package com.colossaldungeons.enhanced.dungeon.puzzle;

import java.util.List;
import java.util.Objects;

/**
 * Utility class for validating puzzle solutions.
 * Provides generic validation logic for sequences, patterns, and comparisons.
 */
public final class PuzzleValidator {

    private PuzzleValidator() {
        // Utility class
    }

    /**
     * Validates that the player's sequence matches the solution sequence exactly.
     *
     * @param playerSequence the sequence of inputs the player provided
     * @param solution the correct solution sequence
     * @return true if sequences match exactly
     */
    public static boolean validateSequence(List<String> playerSequence, List<String> solution) {
        if (playerSequence == null || solution == null) return false;
        if (playerSequence.size() != solution.size()) return false;

        for (int i = 0; i < solution.size(); i++) {
            if (!Objects.equals(playerSequence.get(i), solution.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that the player's sequence is a valid partial match so far.
     * Returns true if the player sequence is a prefix of the solution.
     *
     * @param playerSequence the sequence of inputs so far
     * @param solution the full solution sequence
     * @return true if the sequence is a valid prefix
     */
    public static boolean validatePartialSequence(List<String> playerSequence, List<String> solution) {
        if (playerSequence == null || solution == null) return false;
        if (playerSequence.size() > solution.size()) return false;

        for (int i = 0; i < playerSequence.size(); i++) {
            if (!Objects.equals(playerSequence.get(i), solution.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that the given numeric value matches the target within tolerance.
     *
     * @param value the value to check
     * @param target the target value
     * @param tolerance acceptable deviation
     * @return true if value is within tolerance of target
     */
    public static boolean validateWeight(double value, double target, double tolerance) {
        return Math.abs(value - target) <= tolerance;
    }

    /**
     * Validates a pattern match where the player provides values at specific positions.
     * The pattern uses null entries for "don't care" positions.
     *
     * @param playerPattern the player's pattern values
     * @param solutionPattern the required pattern (null = any value accepted)
     * @return true if the pattern matches
     */
    public static boolean validatePattern(List<String> playerPattern, List<String> solutionPattern) {
        if (playerPattern == null || solutionPattern == null) return false;
        if (playerPattern.size() != solutionPattern.size()) return false;

        for (int i = 0; i < solutionPattern.size(); i++) {
            String required = solutionPattern.get(i);
            if (required != null && !required.isEmpty() && !required.equals(playerPattern.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a sequence contains a wrong input relative to the solution.
     * Returns the index of the first wrong element, or -1 if all are correct so far.
     *
     * @param playerSequence the sequence to check
     * @param solution the correct solution
     * @return index of first wrong element, or -1 if all correct
     */
    public static int findFirstError(List<String> playerSequence, List<String> solution) {
        if (playerSequence == null || solution == null) return 0;

        int checkLength = Math.min(playerSequence.size(), solution.size());
        for (int i = 0; i < checkLength; i++) {
            if (!Objects.equals(playerSequence.get(i), solution.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
