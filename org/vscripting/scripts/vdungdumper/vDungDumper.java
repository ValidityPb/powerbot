package org.vscripting.scripts.vdungdumper;

import org.powerbot.core.script.job.LoopTask;
import org.powerbot.core.script.util.Random;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Environment;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.node.GroundItems;
import org.powerbot.game.api.methods.node.SceneEntities;
import org.powerbot.game.api.util.Filter;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.node.GroundItem;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.node.SceneObject;
import org.vscripting.framework.ActiveScript;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Manifest(authors = "Validity", name = "vDungDumper", description = "Dumps dungeoneering data", version = 1.0)
public class vDungDumper extends ActiveScript {

	private int max = 1;

	private final Map<String, TreeSet<Integer>> objects = new TreeMap<>();
	private final Map<String, TreeSet<Integer>> npcs = new TreeMap<>();
	private final Map<String, TreeSet<Integer>> items = new TreeMap<>();

	private final Filter<SceneObject> ACCEPTABLE_OBJECTS = new Filter<SceneObject>() {
		@Override
		public boolean accept(final SceneObject object) {
			return object != null && object.getDefinition() != null && object.getDefinition().getName() != null && !object.getDefinition().getName().equals("") && !object.getDefinition().getName().equals("null");
		}
	};

	private final Filter<NPC> ACCEPTABLE_NPCS = new Filter<NPC>() {
		@Override
		public boolean accept(final NPC npc) {
			return npc != null && npc.getName() != null && !npc.getName().equals("") && !npc.getName().equals("null");
		}
	};

	private final Filter<GroundItem> ACCEPTABLE_GROUND_ITEMS = new Filter<GroundItem>() {
		@Override
		public boolean accept(final GroundItem item) {
			return item != null && item.getGroundItem() != null && item.getGroundItem().getName() != null && !item.getGroundItem().getName().equals("") && !item.getGroundItem().getName().equals("null");
		}
	};

	@Override
	public boolean setup() {
		final File folder = Environment.getStorageDirectory();
		if (folder.exists()) {
			final File[] files = folder.listFiles();
			if (files != null && files.length > 0) {
				for (final File file : files) {
					if (file != null) {
						final String[] split = file.getName().split("Dump #")[1].split(".txt");
						final int num = Integer.parseInt(split[0]);
						if (num > max) {
							max = num;
						}
					}
				}
				if (max >= 1) {
					try (final BufferedReader reader = new BufferedReader(new FileReader(new File(Environment.getStorageDirectory() + File.separator + "Dump #" + max + ".txt")))) {
						int index = -1;
						String line;
						while ((line = reader.readLine()) != null) {
							if (line.contains("SceneObjects:") || line.contains("NPCs:") || line.contains("Items/GroundItems:")) {
								index++;
								continue;
							} else if (line.equals("")) {
								continue;
							}
							final String[] split = line.split("\\[");
							final String name = split[0].substring(3, split[0].length() - 2);
							final String[] numbers = split[1].split("]")[0].split(", ");
							final TreeSet<Integer> ids = new TreeSet<>();
							for (final String number : numbers) {
								ids.add(Integer.parseInt(number));
							}
							final Map<String, TreeSet<Integer>> selected = index == 0 ? objects : index == 1 ? npcs : items;
							selected.put(name, ids);
						}
						reader.close();
						log.info("Loaded " + objects.size() + " objects, " + npcs.size() + " npcs, and " + items.size() + " items");
					} catch (final IOException ignored) {
						log.info("We couldn't load the last dump file");
					}
					max++;
				}
			}
		}
		submit(new ObjectDumper(), new NpcDumper(), new ItemDumper());
		return true;
	}

	private class ObjectDumper extends LoopTask {

		@Override
		public int loop() {
			addEntries(0, getObjects(SceneEntities.getLoaded(ACCEPTABLE_OBJECTS)));
			return Random.nextInt(500, 750);
		}
	}

	private class NpcDumper extends LoopTask {

		@Override
		public int loop() {
			addEntries(1, getNpcs(NPCs.getLoaded(ACCEPTABLE_NPCS)));
			return Random.nextInt(500, 750);
		}
	}

	private class ItemDumper extends LoopTask {

		@Override
		public int loop() {
			addEntries(2, getGroundItems(GroundItems.getLoaded(ACCEPTABLE_GROUND_ITEMS)));
			return Random.nextInt(500, 750);
		}
	}

	private Map<String, TreeSet<Integer>> getObjects(final SceneObject[] objects) {
		if (objects == null || objects.length == 0) {
			return new HashMap<>();
		}
		final Map<String, TreeSet<Integer>> transformed = new HashMap<>();
		for (final SceneObject object : objects) {
			final String name = object.getDefinition().getName();
			final TreeSet<Integer> ids = transformed.get(name) != null ? transformed.get(name) : new TreeSet<Integer>();
			ids.add(object.getId());
			transformed.put(name, ids);
		}
		return transformed;
	}

	private Map<String, TreeSet<Integer>> getNpcs(final NPC[] npcs) {
		if(npcs == null || npcs.length == 0) {
			return new HashMap<>();
		}
		final Map<String, TreeSet<Integer>> transformed = new HashMap<>();
		for (final NPC npc : npcs) {
			final String name = npc.getName();
			final TreeSet<Integer> ids = transformed.get(name) != null ? transformed.get(name) : new TreeSet<Integer>();
			ids.add(npc.getId());
			transformed.put(name, ids);
		}
		return transformed;
	}

	private Map<String, TreeSet<Integer>> getGroundItems(final GroundItem[] items) {
		if (items == null || items.length == 0) {
			return new HashMap<>();
		}
		final Map<String, TreeSet<Integer>> transformed = new HashMap<>();
		for (final GroundItem item : items) {
			final Item i = item.getGroundItem();
			final String name = i.getName();
			final TreeSet<Integer> ids = transformed.get(name) != null ? transformed.get(name) : new TreeSet<Integer>();
			ids.add(i.getId());
			transformed.put(name, ids);
		}
		return transformed;
	}

	private void addEntries(final int index, final Map<String, TreeSet<Integer>> items) {
		for (final String n1 : items.keySet()) {
			final Set<Integer> ids1 = items.get(n1);
			final Map<String, TreeSet<Integer>> selected = index == 0 ? objects : index == 1 ? npcs : this.items;
			final TreeSet<Integer> ids2 = selected.get(n1) != null ? selected.get(n1) : new TreeSet<Integer>();
			for (final int i : ids1) {
				ids2.add(i);
			}
			selected.put(n1, ids2);
		}
	}

	private void write(final BufferedWriter writer, final int index, final boolean newline) {
		final Set<Map.Entry<String, TreeSet<Integer>>> entries = index == 0 ? objects.entrySet() : index == 1 ? npcs.entrySet() : items.entrySet();
		try {
			writer.write(index == 0 ? "SceneObjects:" : index == 1 ? "NPCs:" : "Items/GroundItems:");
			writer.newLine();
			writer.newLine();
			for (final Map.Entry<String, TreeSet<Integer>> entry : entries) {
				final StringBuilder builder = new StringBuilder();
				builder.append('[');
				final Integer[] ids = entry.getValue().toArray(new Integer[entry.getValue().size()]);
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						builder.append(ids[i]).append(']');
					} else {
						builder.append(ids[i]).append(", ");
					}
				}
				writer.write("\t> " + entry.getKey() + ": " + builder.toString());
				writer.newLine();
			}
			if (newline) {
				writer.newLine();
				writer.newLine();
			}
		} catch (final IOException ignored) {
			log.info("We were unable to save the latest data");
		}
	}

	@Override
	public void onStop() {
		final File save = new File(Environment.getStorageDirectory() + File.separator + "Dump #" + max + ".txt");
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(save, false))) {
			write(writer, 0, true);
			write(writer, 1, true);
			write(writer, 2, false);
			writer.close();
		} catch (final IOException ignored) {
			log.info("We were unable to save the latest data file");
		}
		save.setReadOnly();
	}
}