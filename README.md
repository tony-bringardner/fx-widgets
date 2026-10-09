# fx-widgets

Reusable JavaFX components, the JavaFX counterpart of
[swing-widgets](https://github.com/tony-bringardner/swing-widgets). Only what JavaFX lacks is here;
where JavaFX already has something (CSS styling, `Alert`, prompt text, `Task` progress) there is no
twin of the Swing widget.

- **Java 17** or later (JavaFX 21 needs it)
- Depends on JavaFX 21 (`javafx-controls`) and swing-widgets, for the UI-free list behind the menu
- Apache License 2.0

## Getting started

```xml
<dependency>
    <groupId>us.bringardner</groupId>
    <artifactId>fx-widgets</artifactId>
    <version>1.0.0</version>
</dependency>
```

## What's inside

| Package | Class | Purpose |
|---|---|---|
| `us.bringardner.fx.menu` | `RecentItemsMenu` | A "Recent ..." menu of any kind of item, saved with `java.util.prefs.Preferences`. |

### RecentItemsMenu

The twin of swing-widgets' `RecentItemsMenu`. Both show swing-widgets' `RecentItems`, which uses no
UI toolkit, so a Swing and a JavaFX menu given the same preferences node share one list. Subclasses
override the same methods (`getLabel`, `isStale`, `merge`, `openItem`, `getMaxItemsText`).

```java
RecentItemsMenu<String> recent = new RecentItemsMenu<>("Open Recent", prefs, codec);
recent.setOnOpened(value -> open((String) value));
...
recent.addEntry(path);
```

Differences from the Swing menu:

- A JavaFX `Menu` already has `getItems()` (its menu items), so the list's methods are
  `getEntries()`, `setEntries(...)` and `addEntry(...)`.
- The value `openItem` returns goes to `setOnOpened(...)` instead of to ActionListeners.
- The maximum is asked for by `askMaxItems`, and errors shown by `showError`; tests override them
  instead of showing dialogs.

## Building

```bash
mvn package
```

The tests start JavaFX, so they need a display; where JavaFX can't start they're skipped.

## License

Apache License 2.0. See [LICENSE](LICENSE).
