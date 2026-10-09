/**
 * <PRE>
 *
 * Copyright 1998-2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *
 *
 *	@author Tony Bringardner
 */
package us.bringardner.fx.menu;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import us.bringardner.swing.menu.RecentItems;

/**
 * A "Recent ..." menu, newest first, saved with java.util.prefs.Preferences. The JavaFX twin of
 * swing-widgets' {@link us.bringardner.swing.menu.RecentItemsMenu}: both show a {@link RecentItems},
 * so given the same preferences node they share one list.
 * <p>
 * The menu starts with an item to set the maximum number of entries and one to clear the list,
 * followed by one item per entry. Choosing an entry calls {@link #openItem(Object)}, moves the
 * entry to the top and passes the value openItem returned to {@link #onOpenedProperty() onOpened}.
 * <p>
 * Subclasses can change the label shown for an item ({@link #getLabel(Object)}), drop items that
 * no longer exist ({@link #isStale(Object)}), and keep state from the entry an item replaces
 * ({@link #merge(Object, Object)}). The list is read and the menu is built by the constructor, so
 * these methods are called before a subclass's fields are set; they should not use them.
 * <p>
 * Use it on the JavaFX thread.
 *
 * @param <T> the type of item in the list
 */
public class RecentItemsMenu<T> extends Menu {

	private final RecentItems<T> list;
	private final ObjectProperty<Consumer<Object>> onOpened = new SimpleObjectProperty<>(this, "onOpened");

	/**
	 * Reads the list from the preferences node and builds the menu.
	 *
	 * @param title the menu's text, e.g. "Recent Files"
	 * @param prefs where the list is saved
	 * @param codec turns items into text and back
	 * @throws IOException if the list can't be saved after dropping stale items
	 */
	public RecentItemsMenu(String title, Preferences prefs, RecentItems.Codec<T> codec) throws IOException {
		super(title);
		list = new RecentItems<>(prefs, codec);
		buildMenu();
	}

	/** @return the list the menu shows */
	public RecentItems<T> getRecentItems() {
		return list;
	}

	/** @return the entries, newest first */
	public List<T> getEntries() {
		return list.getItems();
	}

	/** Replaces the list. Duplicates are left out. */
	public void setEntries(List<T> items) throws IOException {
		list.setItems(items);
		buildMenu();
	}

	/**
	 * Puts an item at the top of the list. If the list already has it, the old entry is
	 * replaced by {@link #merge(Object, Object)}. The oldest entries over the maximum are dropped.
	 */
	public void addEntry(T item) throws IOException {
		list.add(item, this::merge);
		buildMenu();
	}

	public int getMaxItems() {
		return list.getMaxItems();
	}

	/** Sets the maximum number of entries. It applies the next time an item is added. */
	public void setMaxItems(int max) throws IOException {
		list.setMaxItems(max);
		buildMenu();
	}

	/** Called with the value {@link #openItem(Object)} returned when the user chooses an entry. */
	public ObjectProperty<Consumer<Object>> onOpenedProperty() {
		return onOpened;
	}

	public Consumer<Object> getOnOpened() {
		return onOpened.get();
	}

	public void setOnOpened(Consumer<Object> handler) {
		onOpened.set(handler);
	}

	/** @return the text shown for an item. The default is its toString(). */
	protected String getLabel(T item) {
		return String.valueOf(item);
	}

	/** @return true if the item no longer exists and should be dropped. The default is false. */
	protected boolean isStale(T item) {
		return false;
	}

	/**
	 * Called by {@link #addEntry(Object)} when the list already has the item.
	 *
	 * @param newer the item being added
	 * @param older the entry it replaces
	 * @return the entry to keep. The default is newer.
	 */
	protected T merge(T newer, T older) {
		return newer;
	}

	/**
	 * Called when the user chooses an entry.
	 *
	 * @return the value passed to onOpened, or null to do nothing. The default is the item.
	 * @throws IOException shown to the user as an error
	 */
	protected Object openItem(T item) throws IOException {
		return item;
	}

	/** @return the text of the item that sets the maximum. */
	protected String getMaxItemsText(int max) {
		return "Max Items:"+max;
	}

	/** Asks the user for the maximum number of entries. @return the text entered, or empty if cancelled */
	protected Optional<String> askMaxItems(int current) {
		TextInputDialog dialog = new TextInputDialog(Integer.toString(current));
		dialog.setTitle("Max Items");
		dialog.setHeaderText(null);
		dialog.setContentText("Max Items");
		return dialog.showAndWait();
	}

	/** Shows an error to the user. */
	protected void showError(String message, Exception e) {
		Alert alert = new Alert(Alert.AlertType.ERROR, String.valueOf(e));
		alert.setHeaderText(message);
		alert.showAndWait();
	}

	private void buildMenu() throws IOException {
		final int mx = getMaxItems();
		MenuItem max = new MenuItem(getMaxItemsText(mx));
		max.setOnAction(e->askMaxItems(mx).ifPresent(text->{
			try {
				setMaxItems(Integer.parseInt(text.trim()));
			} catch (NumberFormatException e2) {
				// not a number: unchanged
			} catch (IOException e2) {
				showError("Can't save the maximum", e2);
			}
		}));
		MenuItem clear = new MenuItem("Clear Recent List");
		clear.setOnAction(e->{
			try {
				list.clear();
				buildMenu();
			} catch (IOException e1) {
				showError("Can't clear the recent list", e1);
			}
		});

		list.removeIf(this::isStale);

		getItems().setAll(max, clear);
		for(T entry : list.getItems()) {
			MenuItem item = new MenuItem(getLabel(entry));
			// the label is text, not a mnemonic: a file name may hold an underscore
			item.setMnemonicParsing(false);
			item.setOnAction(e->open(entry));
			getItems().add(item);
		}
	}

	private void open(T entry) {
		Object value;
		try {
			value = openItem(entry);
		} catch (IOException | RuntimeException e1) {
			showError("Can't open "+getLabel(entry), e1);
			return;
		}
		if( value == null ) {
			return;
		}
		try {
			addEntry(entry);
		} catch (IOException e1) {
			showError("Can't save the recent list", e1);
		}
		Consumer<Object> handler = onOpened.get();
		if( handler != null ) {
			handler.accept(value);
		}
	}
}
