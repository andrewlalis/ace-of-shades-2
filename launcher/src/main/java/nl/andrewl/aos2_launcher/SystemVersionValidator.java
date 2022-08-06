package nl.andrewl.aos2_launcher;

public class SystemVersionValidator {
	private static final String os = System.getProperty("os.name").trim().toLowerCase();
	private static final String arch = System.getProperty("os.arch").trim().toLowerCase();

	private static final boolean OS_WINDOWS = os.contains("win");
	private static final boolean OS_MAC = os.contains("mac");
	private static final boolean OS_LINUX = os.contains("nix") || os.contains("nux") || os.contains("aix");

	private static final boolean ARCH_X86 = arch.equals("x86");
	private static final boolean ARCH_X86_64 = arch.equals("x86_64");
	private static final boolean ARCH_AMD64 = arch.equals("amd64");
	private static final boolean ARCH_AARCH64 = arch.equals("aarch64");
	private static final boolean ARCH_ARM = arch.equals("arm");
	private static final boolean ARCH_ARM32 = arch.equals("arm32");

	public static String getPreferredVersionSuffix() {
		if (OS_LINUX) {
			if (ARCH_AARCH64) return "linux-aarch64";
			if (ARCH_AMD64) return "linux-amd64";
			if (ARCH_ARM) return "linux-arm";
			if (ARCH_ARM32) return "linux-arm32";
		} else if (OS_MAC) {
			if (ARCH_AARCH64) return "macos-aarch64";
			if (ARCH_X86_64) return "macos-x86_64";
		} else if (OS_WINDOWS) {
			if (ARCH_AARCH64) return "windows-aarch64";
			if (ARCH_AMD64) return "windows-amd64";
			if (ARCH_X86) return "windows-x86";
		}
		System.err.println("Couldn't determine the preferred OS/ARCH version. Defaulting to windows-amd64.");
		return "windows-amd64";
	}
}
