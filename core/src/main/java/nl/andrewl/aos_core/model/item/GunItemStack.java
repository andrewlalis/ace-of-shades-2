package nl.andrewl.aos_core.model.item;

public class GunItemStack extends ItemStack {
	private int bulletCount;
	private int clipCount;

	public GunItemStack(Gun gun, int bulletCount, int clipCount) {
		super(gun, 1);
		this.bulletCount = bulletCount;
		this.clipCount = clipCount;
	}

	public GunItemStack(Gun gun) {
		this(gun, gun.getMaxBulletCount(), gun.getMaxClipCount());
	}

	public int getBulletCount() {
		return bulletCount;
	}

	public void setBulletCount(int bulletCount) {
		this.bulletCount = bulletCount;
	}

	public int getClipCount() {
		return clipCount;
	}

	public void setClipCount(int clipCount) {
		this.clipCount = clipCount;
	}
}
