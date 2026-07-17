package com.kenza.callsim.call

/**
 * Compatibility hook for call-lifecycle watcher reset.
 *
 * The active silence / hangup watchers are owned by CallViewModel and are reset
 * directly when a session starts or stops. This package-level hook keeps the
 * call startup path build-safe after the watcher cleanup refactor while leaving
 * the existing runtime behavior untouched.
 */
internal fun resetConversationWatchers() = Unit
