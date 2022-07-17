package nl.andrewl.aos_core.model.item;

public class GunItemStack extends ItemStack {
	private int bulletCount;
	private int clipCount;

	public GunItemStack(Gun gun) {
		super(gun, 1);
		bulletCount = gun.getMaxBulletCount();
		clipCount = gun.getMaxClipCount();
	}
}
