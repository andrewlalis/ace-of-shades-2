package nl.andrewl.aos_core;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public class Directions {
	public static final Vector3ic UP = new Vector3i(0, 1, 0);
	public static final Vector3fc UPf = new Vector3f(0, 1, 0);
	public static final Vector3ic DOWN = new Vector3i(0, -1, 0);
	public static final Vector3ic NEGATIVE_X = new Vector3i(-1, 0, 0);
	public static final Vector3ic POSITIVE_X = new Vector3i(1, 0, 0);
	public static final Vector3ic NEGATIVE_Z = new Vector3i(0, 0, -1);
	public static final Vector3ic POSITIVE_Z = new Vector3i(0, 0, 1);
}
