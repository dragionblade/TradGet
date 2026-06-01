package com.example.tradget;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;

/**
 * Centralized error handling and user-friendly error messages.
 */
public class ErrorHandler {

    public enum ErrorType {
        NETWORK("No internet connection. Please check your connection."),
        FIRESTORE("Database error. Please try again."),
        AUTH("Authentication failed. Please sign in again."),
        INVALID_INPUT("Invalid input. Please check your entries."),
        NOT_FOUND("Resource not found."),
        PERMISSION_DENIED("You don't have permission to perform this action."),
        CONFLICT("This action conflicts with existing data."),
        UNKNOWN("An unexpected error occurred. Please try again.");

        private final String defaultMessage;

        ErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    /**
     * Maps Firebase error messages to user-friendly ErrorType.
     */
    public static ErrorType classifyError(String errorMessage) {
        if (errorMessage == null) return ErrorType.UNKNOWN;

        String lower = errorMessage.toLowerCase();

        if (lower.contains("network") || lower.contains("connection")) {
            return ErrorType.NETWORK;
        } else if (lower.contains("firestore") || lower.contains("database")) {
            return ErrorType.FIRESTORE;
        } else if (lower.contains("auth") || lower.contains("permission")) {
            return ErrorType.PERMISSION_DENIED;
        } else if (lower.contains("not found") || lower.contains("missing")) {
            return ErrorType.NOT_FOUND;
        } else if (lower.contains("invalid") || lower.contains("illegal")) {
            return ErrorType.INVALID_INPUT;
        } else if (lower.contains("already exists") || lower.contains("conflict")) {
            return ErrorType.CONFLICT;
        }

        return ErrorType.UNKNOWN;
    }

    /**
     * Shows a user-friendly error toast.
     */
    public static void showError(Context ctx, String errorMessage) {
        ErrorType type = classifyError(errorMessage);
        showError(ctx, type);
    }

    public static void showError(Context ctx, ErrorType type) {
        if (ctx == null) return;
        Toast.makeText(ctx, "❌ " + type.getDefaultMessage(), Toast.LENGTH_LONG).show();
    }
    
    public static void showError(Context ctx, ErrorType type, String customMessage) {
        if (ctx == null) return;
        String message = customMessage != null && !customMessage.isEmpty() 
            ? customMessage 
            : type.getDefaultMessage();
        Toast.makeText(ctx, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    public static void showSuccess(Context ctx, String message) {
        if (ctx == null) return;
        Toast.makeText(ctx, "✅ " + message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Validates input fields before submission.
     */
    public static boolean validateNotEmpty(String input, String fieldName, Context ctx) {
        if (input == null || input.trim().isEmpty()) {
            showError(ctx, ErrorType.INVALID_INPUT);
            return false;
        }
        return true;
    }

    public static boolean validateEmail(String email, Context ctx) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(ctx, "❌ Invalid email format", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean validatePassword(String password, Context ctx) {
        if (password.length() < 6) {
            Toast.makeText(ctx, "❌ Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
