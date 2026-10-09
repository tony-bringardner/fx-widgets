# fx-widgets

Reusable JavaFX components, the JavaFX counterpart of
[swing-widgets](https://github.com/tony-bringardner/swing-widgets). Only what JavaFX lacks is here;
where JavaFX already has something (CSS styling, `Alert`, prompt text, `Task` progress) there is no
twin of the Swing widget.

- **Java 17** or later (JavaFX 21 needs it)
- Depends on JavaFX 21 (`javafx-controls`) and swing-widgets, for the UI-free list behind the menu
- Apache License 2.0

## Why this library exists

JavaFX is missing a few things a desktop application needs, such as a "recent files" menu, and
[swing-widgets](https://github.com/tony-bringardner/swing-widgets) only helps Swing applications.
fx-widgets fills those gaps for JavaFX, and only those: it doesn't copy Swing widgets that JavaFX
already covers.

It's a separate library from swing-widgets because JavaFX costs more to depend on:

- **Java 17 or later.** JavaFX 21 needs it; swing-widgets runs on Java 11.
- **Native libraries for each platform.** Since Java 11, JavaFX isn't part of the JDK. It runs only
  on the platforms OpenJFX is built for, and an application has to ship it.

Keeping JavaFX here means a Swing application never needs it.

It depends on swing-widgets for one thing: `RecentItems`, the list behind both "recent" menus. It
uses no UI toolkit, so a Swing and a JavaFX menu behave the same and, given the same preferences,
share one list. Depending on swing-widgets doesn't start Swing, and every platform with JavaFX has it
anyway.

**Use fx-widgets** when your application's UI is JavaFX.

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
