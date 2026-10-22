package com.drnitz.aro.domain

/**
 * Explicit result type for domain calculations that can fail because of missing or
 * out-of-range user input.
 *
 * Design rule for the whole domain layer: invalid input is **never** silently replaced
 * with a made-up default. (The web version returned a hardcoded `1800` kcal BMR when
 * weight/height/age were missing, which quietly produced a plausible-looking but wrong
 * target.) The UI can now render [Failure.message] directly to the user.
 */
sealed interface DomainResult<out T> {

    data class Success<out T>(val value: T) : DomainResult<T>

    data class Failure(val reason: ValidationFailure, val message: String) : DomainResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun valueOrNull(): T? = (this as? Success)?.value

    companion object {
        fun <T> of(block: () -> T): DomainResult<T> = try {
            Success(block())
        } catch (e: IllegalArgumentException) {
            Failure(ValidationFailure.OUT_OF_RANGE, e.message ?: "Invalid input")
        }
    }
}

/** Why a domain calculation could not produce a value. */
enum class ValidationFailure {
    /** A required input was null, blank or not yet collected (e.g. before onboarding). */
    MISSING_VALUE,

    /** A numeric input was outside the physiologically meaningful range. */
    OUT_OF_RANGE,

    /** The request cannot be answered from a formula (e.g. 1RM at 25 repetitions). */
    UNSUPPORTED,
}