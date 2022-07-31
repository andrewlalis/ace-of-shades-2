package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.PlayerMode;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.aos_core.model.item.gun.Ak47;
import nl.andrewl.aos_core.model.item.gun.Rifle;
import nl.andrewl.aos_core.model.item.gun.Winchester;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.net.client.*;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Component that manages a server player's current actions and movement.
 */
public class PlayerActionManager {
	private final ServerPlayer player;
	private final PlayerInputTracker input;
	private final PlayerMovementController normalMovementController = new NormalMovementController();
	private final CreativeMovementController creativeMovementController = new CreativeMovementController();

	private long lastBlockRemovedAt = 0;
	private long lastBlockPlacedAt = 0;

	private long lastResupplyAt = System.currentTimeMillis();

	private long gunLastShotAt = 0;
	private boolean gunNeedsReCock = false;
	private boolean gunReloading = false;
	private long gunReloadingStartedAt = 0;
	private GunItemStack reloadingItemStack = null;

	private boolean updated = false;

	public PlayerActionManager(ServerPlayer player) {
		this.player = player;
		this.input = new PlayerInputTracker(player);
	}

	public PlayerInputTracker getInput() {
		return input;
	}

	public boolean setLastInputState(ClientInputState lastInputState) {
		return input.setLastInputState(lastInputState);
	}

	public boolean isScopeEnabled() {
		return input.interacting() &&
				player.getInventory().getSelectedItemStack() instanceof GunItemStack;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void tick(long now, float dt, World world, Server server) {
		updated = false; // Reset the updated flag. This will be set to true if the player was updated in this tick.
		if (player.getInventory().getSelectedIndex() != input.selectedInventoryIndex()) {
			player.getInventory().setSelectedIndex(input.selectedInventoryIndex());
			// Tell the client that their inventory slot has been updated properly.
			server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new InventorySelectedStackMessage(player.getInventory().getSelectedIndex()));
			updated = true; // Tell everyone else that this player's selected item has changed.
		}

		ItemStack selectedStack = player.getInventory().getSelectedItemStack();
		if (selectedStack instanceof BlockItemStack b) {
			tickBlockAction(now, server, world, b);
		} else if (selectedStack instanceof GunItemStack g) {
			tickGunAction(now, server, g);
		}

		if (
				now - lastResupplyAt > server.getConfig().actions.resupplyCooldown * 1000 &&
				player.getTeam() != null &&
				player.getPosition().distance(player.getTeam().getSpawnPoint()) < server.getConfig().actions.resupplyRadius
		) {
			server.getPlayerManager().resupply(player);
			lastResupplyAt = now;
		}

		if (player.getMode() == PlayerMode.NORMAL && server.getConfig().actions.healthRegenPerSecond != 0 && player.getHealth() < 1) {
			player.setHealth(player.getHealth() + server.getConfig().actions.healthRegenPerSecond * dt);
			server.getPlayerManager().getHandler(player).sendDatagramPacket(new ClientHealthMessage(player.getHealth()));
		}

		if (player.isCrouching() != input.crouching()) {
			player.setCrouching(input.crouching());
			updated = true;
		}

		updated = switch (player.getMode()) {
			case NORMAL -> normalMovementController.tickMovement(dt, player, input, server, world, server.getConfig().physics);
			case CREATIVE -> creativeMovementController.tickMovement(dt, player, input, server, world, server.getConfig().physics);
			case SPECTATOR -> false;
		} || updated;
		input.reset(); // Reset our input state after processing this tick's player input.
	}

	private void tickGunAction(long now, Server server, GunItemStack g) {
		Gun gun = (Gun) g.getType();
		if (// Check to see if the player is shooting.
				input.hitting() &&
				g.getBulletCount() > 0 &&
				!gunReloading &&
				now - gunLastShotAt > gun.getShotCooldownTime() * 1000 &&
				(gun.isAutomatic() || !gunNeedsReCock) &&
				!server.getTeamManager().isProtected(player) // Don't allow players to shoot from within their own team's protected zones.
		) {
			shootGun(now, server, gun, g);
		}

		// Check to see if the player is reloading.
		if (input.reloading() && !gunReloading && g.getClipCount() > 0) {
			reloadGun(now, server, g);
		}

		// Check to see if reloading is done.
		if (gunReloading && reloadingItemStack != null && now - gunReloadingStartedAt > gun.getReloadTime() * 1000) {
			reloadingComplete(server, gun);
		}

		// Check to see if the player released the trigger, for non-automatic weapons.
		if (!gun.isAutomatic() && gunNeedsReCock && !input.hitting()) {
			gunNeedsReCock = false;
		}
	}

	private void tickBlockAction(long now, Server server, World world, BlockItemStack stack) {
		// Check for breaking blocks.
		if (
				input.hitting() &&
				stack.getAmount() < stack.getType().getMaxAmount() &&
				now - lastBlockRemovedAt > server.getConfig().actions.blockBreakCooldown * 1000
		) {
			float reach = server.getConfig().actions.blockBreakReach;
			if (player.getMode() == PlayerMode.CREATIVE) reach *= 10;
			var hit = world.getLookingAtPos(player.getEyePosition(), player.getViewVector(), reach);
			if (hit != null && !server.getTeamManager().isProtected(hit.pos())) {
				world.setBlockAt(hit.pos().x, hit.pos().y, hit.pos().z, (byte) 0);
				lastBlockRemovedAt = now;
				if (player.getMode() == PlayerMode.NORMAL) {
					stack.incrementAmount();
					server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ItemStackMessage(player.getInventory()));
				}
				server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(hit.pos(), world));
				server.getPlayerManager().broadcastUdpMessage(new SoundMessage("block_break_1", 1, player.getPosition()));
			}
		}
		// Check for placing blocks.
		if (
				input.interacting() &&
				stack.getAmount() > 0 &&
				now - lastBlockPlacedAt > server.getConfig().actions.blockPlaceCooldown * 1000
		) {
			float reach = server.getConfig().actions.blockPlaceReach;
			if (player.getMode() == PlayerMode.CREATIVE) reach *= 10;
			var hit = world.getLookingAtPos(player.getEyePosition(), player.getViewVector(), reach);
			if (hit != null && !server.getTeamManager().isProtected(hit.pos())) {
				Vector3i placePos = new Vector3i(hit.pos());
				placePos.add(hit.norm());
				boolean canPlace = server.getPlayerManager().getPlayers().stream()
						.noneMatch(p -> p.isSpaceOccupied(placePos)) &&
						world.containsPoint(placePos);
				if (canPlace) { // Ensure that we can't place blocks in space we're occupying.
					world.setBlockAt(placePos.x, placePos.y, placePos.z, stack.getSelectedValue());
					lastBlockPlacedAt = now;
					if (player.getMode() == PlayerMode.NORMAL) {
						stack.decrementAmount();
						server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ItemStackMessage(player.getInventory()));
					}
					server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(placePos, world));
					server.getPlayerManager().broadcastUdpMessage(new SoundMessage("block_place_1", 1, player.getPosition()));
				}
			}
		}
	}

	private void shootGun(long now, Server server, Gun gun, GunItemStack g) {
		server.getProjectileManager().spawnBullets(player, gun);
		g.setBulletCount(g.getBulletCount() - 1);
		gunLastShotAt = now;
		if (!gun.isAutomatic()) {
			gunNeedsReCock = true;
		}
		server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ItemStackMessage(player.getInventory()));
		// Apply recoil!
		float recoilFactor = 10f; // Maximum number of degrees to recoil.
		if (isScopeEnabled()) recoilFactor *= 0.1f;
		float recoil = recoilFactor * gun.getRecoil() + (float) ThreadLocalRandom.current().nextGaussian(0, 0.01);
		server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ClientRecoilMessage(0, Math.toRadians(recoil)));
		// Play sound!
		String shotSound = null;
		if (gun instanceof Rifle) {
			shotSound = "shot_m1-garand_1";
		} else if (gun instanceof Ak47) {
			shotSound = "shot_ak-47_1";
		} else if (gun instanceof Winchester) {
			shotSound = "shot_winchester_1";
		}
		Vector3f soundLocation = new Vector3f(player.getPosition());
		soundLocation.y += 1.4f;
		soundLocation.add(player.getViewVector());
		server.getPlayerManager().broadcastUdpMessage(new SoundMessage(shotSound, 1, soundLocation, player.getVelocity()));
	}

	private void reloadGun(long now, Server server, GunItemStack g) {
		g.setClipCount(g.getClipCount() - 1);
		gunReloadingStartedAt = now;
		gunReloading = true;
		reloadingItemStack = g;
		server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ItemStackMessage(player.getInventory()));
		server.getPlayerManager().broadcastUdpMessage(new SoundMessage("reload", 1, player.getPosition(), player.getVelocity()));
	}

	private void reloadingComplete(Server server, Gun gun) {
		reloadingItemStack.setBulletCount(gun.getMaxBulletCount());
		int idx = player.getInventory().getIndex(reloadingItemStack);
		if (idx != -1) {
			server.getPlayerManager().getHandler(player.getId()).sendDatagramPacket(new ItemStackMessage(idx, reloadingItemStack));
		}
		gunReloading = false;
		reloadingItemStack = null;
	}
}
