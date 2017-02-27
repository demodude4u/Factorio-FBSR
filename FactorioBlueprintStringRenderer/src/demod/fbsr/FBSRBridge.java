package demod.fbsr;

import java.util.function.Consumer;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class FBSRBridge extends TwoArgFunction {

	private static String blueprintDecoded;
	private static Consumer<LuaTable> blueprintListener;

	public static void setBlueprintDecoded(String blueprintDecoded) {
		FBSRBridge.blueprintDecoded = blueprintDecoded;
	}

	public static void setBlueprintListener(Consumer<LuaTable> blueprintListener) {
		FBSRBridge.blueprintListener = blueprintListener;
	}

	@Override
	public LuaValue call(LuaValue modname, LuaValue env) {
		LuaValue library = tableOf();
		library.set("decoded", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return valueOf(blueprintDecoded);
			}
		});
		library.set("result", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				blueprintListener.accept(arg.checktable());
				return LuaValue.NIL;
			}
		});
		env.set("fbsr", library);
		return library;
	}
}
