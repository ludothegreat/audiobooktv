package xyz.ludothegreat.audiobooktv

/**
 * Marks a test that composes UI through createComposeRule and therefore needs
 * the debug-only ui-test-manifest. The release unit-test variant excludes this
 * category; see the Test task configuration in app/build.gradle.kts.
 */
interface ComposeUiTest
