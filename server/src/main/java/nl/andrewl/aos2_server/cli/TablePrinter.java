package nl.andrewl.aos2_server.cli;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TablePrinter {
	private final PrintWriter out;
	private final List<List<String>> lines = new ArrayList<>();
	private boolean drawBorders = false;
	private final int[] padding = new int[]{1, 1, 0, 0};

	public TablePrinter(PrintWriter out) {
		this.out = out;
	}

	public TablePrinter addLine(List<String> line) {
		lines.add(line);
		return this;
	}

	public TablePrinter addLine(Object... objects) {
		List<String> line = new ArrayList<>(objects.length);
		for (var obj : objects) {
			line.add(obj.toString());
		}
		return addLine(line);
	}

	public TablePrinter drawBorders(boolean drawBorders) {
		this.drawBorders = drawBorders;
		return this;
	}

	public TablePrinter padding(int left, int right, int top, int bottom) {
		this.padding[0] = left;
		this.padding[1] = right;
		this.padding[2] = top;
		this.padding[3] = bottom;
		return this;
	}

	public void println() {
		int rowCount = lines.size();
		int colCount = lines.stream().mapToInt(List::size).max().orElse(0);
		if (rowCount == 0 || colCount == 0) out.println();

		int[] colSizes = new int[colCount];
		for (int i = 0; i < colCount; i++) {
			final int columnIndex = i;
			colSizes[i] = lines.stream().mapToInt(line -> {
				if (columnIndex >= line.size()) return 0;
				return line.get(columnIndex).length();
			}).max().orElse(0);
		}

		for (int row = 0; row < rowCount; row++) {
			// Row top border.
			if (drawBorders) {
				out.print('+');
				for (int col = 0; col < colCount; col++) {
					out.print("-".repeat(colSizes[col] + leftPadding() + rightPadding()));
					out.print('+');
				}
				out.println();
			}
			// Top padding rows.
			for (int p = 0; p < topPadding(); p++) {
				for (int col = 0; col < colCount; col++) {
					if (drawBorders) out.print('|');
					out.print(" ".repeat(leftPadding() + colSizes[col] + rightPadding()));
				}
				if (drawBorders) out.print('|');
				out.println();
			}
			// Column values.
			for (int col = 0; col < colCount; col++) {
				String value = getValueAt(row, col);
				if (drawBorders) out.print('|');
				out.print(" ".repeat(leftPadding()));
				out.print(value);
				out.print(" ".repeat(colSizes[col] - value.length() + rightPadding()));
			}
			if (drawBorders) out.print('|');
			out.println();
			// Bottom padding rows.
			for (int p = 0; p < bottomPadding(); p++) {
				for (int col = 0; col < colCount; col++) {
					if (drawBorders) out.print('|');
					out.print(" ".repeat(leftPadding() + colSizes[col] + rightPadding()));
				}
				if (drawBorders) out.print('|');
				out.println();
			}

			// Last row bottom border.
			if (row == rowCount - 1 && drawBorders) {
				out.print('+');
				for (int col = 0; col < colCount; col++) {
					out.print("-".repeat(colSizes[col] + leftPadding() + rightPadding()));
					out.print('+');
				}
				out.println();
			}
		}
	}

	private String getValueAt(int row, int col) {
		if (row < lines.size()) {
			List<String> line = lines.get(row);
			if (col < line.size()) {
				return line.get(col);
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	private int leftPadding() {
		return padding[0];
	}

	private int rightPadding() {
		return padding[1];
	}

	private int topPadding() {
		return padding[2];
	}

	private int bottomPadding() {
		return padding[3];
	}
}
