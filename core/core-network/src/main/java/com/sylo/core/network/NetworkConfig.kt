package com.sylo.core.network

/**
 * Networking constants. In a real build these would come from BuildConfig fields /
 * a secrets provider per flavor; kept here as clearly-marked placeholders.
 */
object NetworkConfig {

    // The base URL now comes from the active product flavor's BuildConfig.API_BASE_URL,
    // provided into the graph via @BaseUrl (see :app AppNetworkModule / NetworkModule).

    /** Host used for certificate pinning (no scheme, no path). */
    const val PINNED_HOST = "api.sylo.example.com"

    /**
     * SPKI SHA-256 pins. Pin at least TWO: the leaf/intermediate currently in use
     * (PRIMARY) and a backup (e.g. the next cert or a different CA) so a routine
     * certificate rotation does not brick every installed client.
     *
     * Generate with:
     *   openssl s_client -connect api.sylo.example.com:443 | \
     *     openssl x509 -pubkey -noout | \
     *     openssl pkey -pubin -outform der | \
     *     openssl dgst -sha256 -binary | openssl enc -base64
     *
     * TODO: replace the placeholder values below with real pins before shipping.
     */
    const val PRIMARY_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    const val BACKUP_PIN = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
}
