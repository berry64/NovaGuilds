/*
 *     NovaGuilds - Bukkit plugin
 *     Copyright (C) 2016 Marcin (CTRL) Wieczorek
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package co.marcin.novaguilds.impl.util;

import co.marcin.novaguilds.NovaGuilds;
import co.marcin.novaguilds.api.basic.GUIInventory;
import co.marcin.novaguilds.api.basic.MessageWrapper;
import co.marcin.novaguilds.api.basic.NovaPlayer;
import co.marcin.novaguilds.util.ChestGUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractGUIInventory implements GUIInventory {
	protected final Inventory inventory;
	private NovaPlayer viewer;
	protected final NovaGuilds plugin = NovaGuilds.getInstance();
	private final Set<GUIInventory.Executor> executors = new HashSet<>();

	/**
	 * The constructor
	 *
	 * @param size  inventory size (multiply of 9)
	 * @param title title message
	 */
	public AbstractGUIInventory(int size, MessageWrapper title) {
		inventory = ChestGUIUtils.createInventory(size, title);
	}

	@Override
	public final NovaPlayer getViewer() {
		return viewer;
	}

	@Override
	public final void setViewer(NovaPlayer nPlayer) {
		this.viewer = nPlayer;
	}

	@Override
	public void registerExecutor(GUIInventory.Executor executor) {
		if(executors.contains(executor)) {
			return;
		}

		executors.add(executor);
	}

	@Override
	public Set<GUIInventory.Executor> getExecutors() {
		return executors;
	}

	@Override
	public void onClick(InventoryClickEvent event) {
		ItemStack clickedItemStack = event.getCurrentItem();

		for(GUIInventory.Executor executor : getExecutors()) {
			if(executor.getItem().equals(clickedItemStack)) {
				executor.execute();
			}
		}
	}

	@Override
	public final Inventory getInventory() {
		return inventory;
	}

	@Override
	public final void open(NovaPlayer nPlayer) {
		setViewer(nPlayer);
		ChestGUIUtils.openGUIInventory(nPlayer, this);
	}

	@Override
	public void onOpen() {

	}

	@Override
	public final void close() {
		getViewer().getPlayer().closeInventory();
	}

	/**
	 * Adds an item if not null
	 *
	 * @param itemStack the itemstack
	 */
	protected void add(ItemStack itemStack) {
		if(itemStack != null) {
			getInventory().addItem(itemStack);
		}
	}

	protected void add(GUIInventory.Executor executor) {
		if(!getExecutors().contains(executor)) {
			throw new IllegalArgumentException("Trying to add not registered executor to the inventory");
		}

		add(executor.getItem());
	}

	protected void registerAndAdd(GUIInventory.Executor executor) {
		registerExecutor(executor);
		add(executor);
	}

	/**
	 * Reopens the GUI
	 */
	protected void reopen() {
		close();
		open(getViewer());
	}

	public abstract class Executor implements GUIInventory.Executor {
		private ItemStack itemStack;

		/**
		 * The constructor
		 *
		 * @param itemStack icon item
		 */
		public Executor(ItemStack itemStack) {
			this.itemStack = itemStack;
		}

		/**
		 * The constructor
		 * ItemStack is generated from message
		 *
		 * @param messageWrapper message wrapper
		 */
		public Executor(MessageWrapper messageWrapper) {
			this(messageWrapper.getItemStack());
		}

		@Override
		public ItemStack getItem() {
			return itemStack;
		}
	}

	public class EmptyExecutor extends Executor {
		public EmptyExecutor(ItemStack itemStack) {
			super(itemStack);
		}

		public EmptyExecutor(MessageWrapper messageWrapper) {
			super(messageWrapper);
		}

		@Override
		public final void execute() {

		}
	}

	public class CommandExecutor extends Executor {
		private final String command;

		public CommandExecutor(ItemStack itemStack, String command) {
			super(itemStack);
			this.command = command;
		}

		public CommandExecutor(MessageWrapper messageWrapper, String command) {
			this(messageWrapper.getItemStack(), command);
		}

		@Override
		public void execute() {
			Bukkit.dispatchCommand(getViewer().getPlayer(), command);
		}
	}
}
