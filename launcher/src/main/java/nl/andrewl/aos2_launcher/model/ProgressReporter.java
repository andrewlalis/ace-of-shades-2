package nl.andrewl.aos2_launcher.model;

public interface ProgressReporter {
	void enableProgress();
	void disableProgress();
	void setActionText(String text);
	void setProgress(double progress);
}
