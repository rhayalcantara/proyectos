pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WhatsAppClone"

include(":app")

// Core modules
include(":core:network")
include(":core:database")
include(":core:common")
include(":core:ui")

// Domain
include(":domain")

// Feature modules
include(":feature:auth")
include(":feature:chats")
include(":feature:chat")
include(":feature:calls")
include(":feature:status")
include(":feature:contacts")
include(":feature:profile")
include(":feature:settings")
include(":feature:main")
