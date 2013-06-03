package org.vscripting.framework;

import org.powerbot.core.event.listeners.PaintListener;
import org.powerbot.core.script.job.Job;
import org.powerbot.core.script.job.state.Node;
import org.powerbot.core.script.job.state.Tree;
import org.powerbot.core.script.methods.Game;
import org.powerbot.core.script.util.Random;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ActiveScript extends org.powerbot.core.script.ActiveScript implements PaintListener {

	private Tree container = null;
	private final List<Node> jobs = Collections.synchronizedList(new ArrayList<Node>());

	public abstract boolean setup();

	public void provide(final Node... jobs) {
		for (final Node job : jobs) {
			if (!this.jobs.contains(job)) {
				this.jobs.add(job);
			}
		}
		container = new Tree(this.jobs.toArray(new Node[this.jobs.size()]));
	}

	public void submit(final Job... jobs) {
		for (final Job job : jobs) {
			getContainer().submit(job);
		}
	}

	@Override
	public void onStart() {
		if (!setup()) {
			stop();
		}
	}

	@Override
	public int loop() {
		if (Game.isLoggedIn()) {
			if (container != null) {
				final Node job = container.state();
				if (job != null) {
					container.set(job);
					getContainer().submit(job);
					job.join();
				}
			}
		}
		return Random.nextInt(200, 400);
	}

	@Override
	public void onRepaint(final Graphics graphics) {
	}
}