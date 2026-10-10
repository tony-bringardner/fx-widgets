# Changelog

## fx-widgets 1.0.0 (unreleased)

### Changed

- Runs on Java 11, like the library's other components (was Java 17), with JavaFX 17 LTS
  (17.0.20; was JavaFX 21, which needs Java 17).

### Added

- `us.bringardner.fx.menu.RecentItemsMenu`, a "Recent ..." menu of any kind of item, saved with
  `java.util.prefs.Preferences`: the JavaFX twin of swing-widgets' `RecentItemsMenu`. Both show
  swing-widgets' `RecentItems`, so they share a list.
