package nl.andrewl.aos_core.model.item;

import nl.andrewl.aos_core.MathUtils;

/**
 * The base class for all types of guns.
 */
public class Gun extends Item {
	private final int maxClipCount;
	private final int maxBulletCount;
	private final int bulletsPerRound;
	private final float accuracy;
	private final float shotCooldownTime;
	private final float reloadTime;
	private final float baseDamage;
	private final float recoil;
	private boolean automatic;

	public Gun(
			int id,
			String name,
			int maxClipCount,
			int maxBulletCount,
			int bulletsPerRound,
			float accuracy,
			float shotCooldownTime,
			float reloadTime,
			float baseDamage,
			float recoil,
			boolean automatic
	) {
		super(id, name, 1);
		this.maxClipCount = maxClipCount;
		this.maxBulletCount = maxBulletCount;
		this.bulletsPerRound = bulletsPerRound;
		this.accuracy = MathUtils.clamp(accuracy, 0, 1);
		this.shotCooldownTime = shotCooldownTime;
		this.reloadTime = reloadTime;
		this.baseDamage = baseDamage;
		this.recoil = recoil;
		this.automatic = automatic;
	}

	public int getMaxClipCount() {
		return maxClipCount;
	}

	public int getMaxBulletCount() {
		return maxBulletCount;
	}

	public int getBulletsPerRound() {
		return bulletsPerRound;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public float getShotCooldownTime() {
		return shotCooldownTime;
	}

	public float getReloadTime() {
		return reloadTime;
	}

	public float getBaseDamage() {
		return baseDamage;
	}

	public float getRecoil() {
		return recoil;
	}

	public boolean isAutomatic() {
		return automatic;
	}
}
