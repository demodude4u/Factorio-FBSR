package demod.fbsr;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public final class Utils {
	public static int compareRange(double min1, double max1, double min2, double max2) {
		if (max1 <= min2) {
			return -1;
		}
		if (max2 <= min1) {
			return 1;
		}
		return 0;
	}

	public static void debugPrintTable(LuaValue table) {
		debugPrintTable("", table);
	}

	private static void debugPrintTable(String prefix, LuaValue table) {
		forEach(table, (k, v) -> {
			if (v.istable()) {
				debugPrintTable(prefix + k + ".", v.checktable());
			} else {
				System.out.println(prefix + k + " = " + v);
			}
		});
	}

	public static void forEach(LuaValue table, BiConsumer<LuaValue, LuaValue> consumer) {
		LuaValue k = LuaValue.NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.arg1()).isnil())
				break;
			LuaValue v = n.arg(2);
			consumer.accept(k, v);
		}
	}

	public static void forEach(LuaValue table, Consumer<LuaValue> consumer) {
		LuaValue k = LuaValue.NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.arg1()).isnil())
				break;
			LuaValue v = n.arg(2);
			consumer.accept(v);
		}
	}

	public static Double parsePoint2D(LuaValue luaValue) {
		if (luaValue.isnil()) {
			return new Point2D.Double();
		}
		return new Point2D.Double(luaValue.get(1).checkdouble(), luaValue.get(2).checkdouble());
	}

	public static Rectangle2D.Double parseRectangle(LuaValue value) {
		LuaTable table = value.checktable();
		LuaValue p1 = table.get(1);
		LuaValue p2 = table.get(2);
		double x1 = p1.get(1).checkdouble();
		double y1 = p1.get(2).checkdouble();
		double x2 = p2.get(1).checkdouble();
		double y2 = p2.get(2).checkdouble();
		return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
	}

	public static <T> void sortWithNonTransitiveComparator(T[] array, Comparator<T> comparator) {
		@SuppressWarnings("unchecked")
		T[] tmp = (T[]) new Object[array.length];
		sortWithNonTransitiveComparator_MergeSort(array, comparator, tmp, 0, array.length - 1);
	}

	private static <T> void sortWithNonTransitiveComparator_Merge(T[] a, Comparator<T> comparator, T[] tmp, int left,
			int right, int rightEnd) {
		int leftEnd = right - 1;
		int k = left;
		int num = rightEnd - left + 1;

		while (left <= leftEnd && right <= rightEnd)
			if (comparator.compare(a[left], a[right]) <= 0)
				tmp[k++] = a[left++];
			else
				tmp[k++] = a[right++];

		while (left <= leftEnd) // Copy rest of first half
			tmp[k++] = a[left++];

		while (right <= rightEnd) // Copy rest of right half
			tmp[k++] = a[right++];

		// Copy tmp back
		for (int i = 0; i < num; i++, rightEnd--)
			a[rightEnd] = tmp[rightEnd];
	}

	private static <T> void sortWithNonTransitiveComparator_MergeSort(T[] a, Comparator<T> comparator, T[] tmp,
			int left, int right) {
		if (left < right) {
			int center = (left + right) / 2;
			sortWithNonTransitiveComparator_MergeSort(a, comparator, tmp, left, center);
			sortWithNonTransitiveComparator_MergeSort(a, comparator, tmp, center + 1, right);
			sortWithNonTransitiveComparator_Merge(a, comparator, tmp, left, center + 1, right);
		}
	}

	private Utils() {
	}
}
