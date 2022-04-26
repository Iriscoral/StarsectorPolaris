package data.scripts.util;

import java.util.ArrayList;
import java.util.List;

public class PLSP_BlackList {

	public static boolean CHECKED;
	private static final List<String> BlackListModId = new ArrayList<>();
	private static final List<String> BlackListShipId = new ArrayList<>();
	private static final List<String> BlackListWeaponId = new ArrayList<>();
	static {
		BlackListModId.add("YR");
		BlackListShipId.add("141YR2");
		BlackListShipId.add("141YR1");
		BlackListShipId.add("141YR0");
		BlackListShipId.add("141YR3");
		BlackListShipId.add("141YR4");
		BlackListShipId.add("141YR5");
		BlackListShipId.add("141YR6");
		BlackListShipId.add("141YRF");
		BlackListShipId.add("141YRF1");
		BlackListShipId.add("141YRF2");
		BlackListShipId.add("141YRF3");
		BlackListShipId.add("141YR7");
		BlackListShipId.add("141YR8");
		BlackListShipId.add("141YR9");
		BlackListShipId.add("141YR10");
		BlackListShipId.add("141YR11");
		BlackListShipId.add("141YR12");
		BlackListShipId.add("141YR13");
		BlackListShipId.add("141YR14");
		BlackListShipId.add("141YR15");
		BlackListShipId.add("141YR16");
		BlackListShipId.add("141YR17");
		BlackListShipId.add("14TB1");
		BlackListShipId.add("14TB2");
		BlackListShipId.add("14TB2");
		BlackListShipId.add("ZYLM");
		BlackListShipId.add("ZYLM1");
		BlackListShipId.add("ZYLM2");
		BlackListShipId.add("ZYLM3");
		BlackListShipId.add("ZYLM4");
		BlackListShipId.add("ZYLM5");
		BlackListShipId.add("ZYLM6");
		BlackListShipId.add("ZYLM7");
		BlackListShipId.add("ZYLM8");
		BlackListShipId.add("XINDA");
		BlackListShipId.add("XINDA1");
		BlackListShipId.add("YXZLM");
		BlackListShipId.add("YXZLM1");
		BlackListShipId.add("YXZLM2");
		BlackListShipId.add("YXZLM3");
		BlackListShipId.add("YXZLM4");
		BlackListShipId.add("YXZLM5");
		BlackListShipId.add("YXZLM6");
		BlackListShipId.add("YXZLM7");
		BlackListShipId.add("YXZLM8");
		BlackListShipId.add("YXZLM9");
		BlackListShipId.add("YXZLM10");
		BlackListShipId.add("haidao");
		BlackListShipId.add("YUHUI");
		BlackListShipId.add("YUHUI4");
		BlackListShipId.add("YUHUI1");
		BlackListShipId.add("YUHUI5");
		BlackListShipId.add("YUHUIF");
		BlackListShipId.add("141YR00");
		BlackListShipId.add("141YR00Z");
		BlackListShipId.add("DTL");
		BlackListWeaponId.add("pao");
		BlackListWeaponId.add("daodan");
		BlackListWeaponId.add("yrc7");
		BlackListWeaponId.add("yrc8");
		BlackListWeaponId.add("yrc");
		BlackListWeaponId.add("tb");
		BlackListWeaponId.add("tb2");
		BlackListWeaponId.add("yrc9");
		BlackListWeaponId.add("yrc1");
		BlackListWeaponId.add("yrc2");
		BlackListWeaponId.add("yrc3");
		BlackListWeaponId.add("yrc4");
		BlackListWeaponId.add("yrc5");
		BlackListWeaponId.add("yrf");
		BlackListWeaponId.add("yrc6");
		BlackListWeaponId.add("yrc10");
		BlackListWeaponId.add("yrc11");
		BlackListWeaponId.add("yxz");
		BlackListWeaponId.add("yuhui");
		BlackListWeaponId.add("yrc12");
		BlackListWeaponId.add("yrc-1");
		BlackListWeaponId.add("yrc2-2");
		BlackListWeaponId.add("yrc3-3");
		BlackListWeaponId.add("yry1");
		BlackListWeaponId.add("yry2");
	}

	public static String getCode() {
		CHECKED = true;

		String raw = BlackListModId.toString() + BlackListShipId.toString() + BlackListWeaponId.toString();
		return PLSP_Util.md5(raw);
	}

	public static List<String> getBlackListModId() {
		return new ArrayList<>(BlackListModId);
	}

	public static List<String> getBlackListShipId() {
		return new ArrayList<>(BlackListShipId);
	}

	public static List<String> getBlackListWeaponId() {
		return new ArrayList<>(BlackListWeaponId);
	}
}