package nl.andrewl.aos_core.model;

/**
 * Represents the different modes that a player can be in.
 * <ul>
 *     <li>
 *         In normal mode, the player acts as a usual competitive player that
 *         has an inventory, can shoot weapons, and must traverse the world by
 *         walking around.
 *     </li>
 *     <li>
 *         In creative mode, the player can fly, but still collides with the
 *         world's objects. The player also has unlimited ammunition and
 *         blocks.
 *     </li>
 *     <li>
 *         In spectator mode, the player can fly freely throughout the world,
 *         limited by nothing. The player can't interact with the world in any
 *         way other than simply observing it through sight and sound.
 *     </li>
 * </ul>
 */
public enum PlayerMode {
	NORMAL,
	CREATIVE,
	SPECTATOR
}
