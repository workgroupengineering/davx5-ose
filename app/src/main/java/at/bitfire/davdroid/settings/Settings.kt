package at.bitfire.davdroid.settings

object Settings {

    const val FOREGROUND_SERVICE = "foreground_service"

    const val DISTRUST_SYSTEM_CERTIFICATES = "distrust_system_certs"

    // THEME
    const val PREFERRED_THEME = "preferred_theme"
    // THEME Options, must be kept consistent with @string\app_settings_theme_technical
    const val PREFERRED_THEME_DAY = "DAY"
    const val PREFERRED_THEME_NIGHT = "NIGHT"
    const val PREFERRED_THEME_SYSTEM = "SYSTEM"


    const val OVERRIDE_PROXY = "override_proxy"
    const val OVERRIDE_PROXY_HOST = "override_proxy_host"
    const val OVERRIDE_PROXY_PORT = "override_proxy_port"

    /**
     * Default sync interval (long), in seconds.
     * Used to initialize an account.
     */
    const val DEFAULT_SYNC_INTERVAL = "default_sync_interval"

    const val PREFERRED_TASKS_PROVIDER = "preferred_tasks_provider"

}
