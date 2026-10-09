package us.bringardner.fx.menu.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.scene.control.MenuItem;
import us.bringardner.fx.menu.RecentItemsMenu;
import us.bringardner.swing.menu.RecentItems;

public class TestRecentItemsMenu {

	private static final String BASE = "us/bringardner/fx/menu/test/TestRecentItemsMenu";
	private static final RecentItems.Codec<String> STRINGS = new RecentItems.Codec<String>() {
		@Override
		public String encode(String item) {
			return item;
		}

		@Override
		public String decode(String line) {
			return line;
		}
	};

	/** Items in this set are stale. */
	private static final Set<String> gone = new HashSet<>();

	/** Records what would be shown instead of showing dialogs. */
	static class TestMenu extends RecentItemsMenu<String> {
		final List<String> errors = new ArrayList<>();
		String answer;

		TestMenu(Preferences prefs) throws IOException {
			super("Recent", prefs, STRINGS);
		}

		@Override
		protected String getLabel(String item) {
			return "<"+item+">";
		}

		@Override
		protected boolean isStale(String item) {
			return gone.contains(item);
		}

		@Override
		protected Object openItem(String item) throws IOException {
			if( item.equals("cancel") ) {
				return null;
			}
			if( item.equals("broken") ) {
				throw new IOException("can't read it");
			}
			return item.toUpperCase();
		}

		@Override
		protected Optional<String> askMaxItems(int current) {
			return Optional.ofNullable(answer);
		}

		@Override
		protected void showError(String message, Exception e) {
			errors.add(message+": "+e.getMessage());
		}
	}

	private Preferences node;

	@BeforeEach
	public void setUp() {
		gone.clear();
		node = Preferences.userRoot().node(BASE+"/"+UUID.randomUUID());
	}

	@AfterEach
	public void tearDown() throws BackingStoreException {
		Preferences parent = node.parent();
		node.removeNode();
		parent.flush();
	}

	@AfterAll
	public static void removeBase() throws BackingStoreException {
		Preferences.userRoot().node(BASE).removeNode();
		Preferences.userRoot().flush();
	}

	private static List<String> labels(RecentItemsMenu<?> menu) {
		return menu.getItems().stream().map(MenuItem::getText).collect(Collectors.toList());
	}

	@Test
	public void controlsThenOneItemPerEntry() throws Exception {
		Fx.run(()->{
			TestMenu menu = new TestMenu(node);
			menu.setEntries(Arrays.asList("a", "b", "a"));
			assertEquals(Arrays.asList("Max Items:10", "Clear Recent List", "<a>", "<b>"), labels(menu));

			menu.getItems().get(1).fire();
			assertTrue(menu.getEntries().isEmpty());
			assertEquals(2, menu.getItems().size());
			assertEquals("", node.get(RecentItems.PREF_RECENT_LIST, null));
		});
	}

	@Test
	public void choosingAnEntryMovesItUpAndPassesOnTheOpenedValue() throws Exception {
		Fx.run(()->{
			TestMenu menu = new TestMenu(node);
			menu.setEntries(Arrays.asList("a", "b", "cancel", "broken"));
			List<Object> opened = new ArrayList<>();
			menu.setOnOpened(opened::add);

			menu.getItems().get(3).fire();
			assertEquals(Arrays.asList("B"), opened);
			assertEquals(Arrays.asList("b", "a", "cancel", "broken"), menu.getEntries());
			assertEquals(Arrays.asList("<b>", "<a>", "<cancel>", "<broken>"), labels(menu).subList(2, 6));

			// openItem returning null does nothing
			menu.getItems().get(4).fire();
			assertEquals(1, opened.size());

			// a failure is shown and the list is left as it was
			menu.getItems().get(5).fire();
			assertEquals(Arrays.asList("Can't open <broken>: can't read it"), menu.errors);
			assertEquals(1, opened.size());
			assertEquals(Arrays.asList("b", "a", "cancel", "broken"), menu.getEntries());
		});
	}

	@Test
	public void theMaximumIsAskedForAndKept() throws Exception {
		Fx.run(()->{
			TestMenu menu = new TestMenu(node);
			menu.answer = " 2 ";
			menu.getItems().get(0).fire();
			assertEquals(2, menu.getMaxItems());
			assertEquals("Max Items:2", menu.getItems().get(0).getText());

			menu.answer = "lots";
			menu.getItems().get(0).fire();
			assertEquals(2, menu.getMaxItems());

			menu.answer = null;
			menu.getItems().get(0).fire();
			assertEquals(2, menu.getMaxItems());

			menu.addEntry("1");
			menu.addEntry("2");
			menu.addEntry("3");
			assertEquals(Arrays.asList("3", "2"), menu.getEntries());
			assertEquals(2, new TestMenu(node).getMaxItems());
		});
	}

	@Test
	public void staleEntriesAreDropped() throws Exception {
		node.put(RecentItems.PREF_RECENT_LIST, "a\nb\nc\n");
		gone.add("b");
		Fx.run(()->{
			TestMenu menu = new TestMenu(node);
			assertEquals(Arrays.asList("a", "c"), menu.getEntries());
			assertEquals("a\nc\n", node.get(RecentItems.PREF_RECENT_LIST, null));
		});
	}

	@Test
	public void mergeDecidesWhichEntryIsKept() throws Exception {
		Fx.run(()->{
			String older = new String("x");
			RecentItemsMenu<String> menu = new RecentItemsMenu<String>("Recent", node, STRINGS) {
				@Override
				protected String merge(String newer, String old) {
					return old;
				}
			};
			menu.addEntry(older);
			menu.addEntry("y");
			menu.addEntry(new String("x"));
			assertSame(older, menu.getEntries().get(0));
			assertEquals(2, menu.getEntries().size());
		});
	}

	@Test
	public void labelsAreShownAsWritten() throws Exception {
		Fx.run(()->{
			RecentItemsMenu<String> menu = new RecentItemsMenu<>("Recent", node, STRINGS);
			menu.setEntries(Arrays.asList("my_script.sh"));
			MenuItem item = menu.getItems().get(2);
			assertEquals("my_script.sh", item.getText());
			assertFalse(item.isMnemonicParsing());
		});
	}

	@Test
	public void theListIsSharedWithTheSwingMenusPreferences() throws Exception {
		RecentItems<String> list = new RecentItems<>(node, STRINGS);
		list.setItems(Arrays.asList("from", "swing"));
		Fx.run(()->{
			RecentItemsMenu<String> menu = new RecentItemsMenu<>("Recent", node, STRINGS);
			assertEquals(Arrays.asList("from", "swing"), menu.getEntries());
			menu.addEntry("fx");
		});
		assertEquals(Arrays.asList("fx", "from", "swing"), new RecentItems<>(node, STRINGS).getItems());
	}
}
