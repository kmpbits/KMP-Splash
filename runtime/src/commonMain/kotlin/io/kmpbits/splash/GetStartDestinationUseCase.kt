package io.kmpbits.splash

/** Override this in your app to provide the real start-destination logic. */
fun interface GetStartDestinationUseCase {
    suspend operator fun invoke(): FirstDestination
}
