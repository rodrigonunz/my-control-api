package com.mycontrol.api.utils;

import java.math.BigDecimal;

public class MathUtils {

	public static boolean equals(BigDecimal a, BigDecimal b) {
		if(a == null || b == null)
			return false;
		int maxScale = Math.max(a.scale(), b.scale());
		a = a.setScale(maxScale);
		b = b.setScale(maxScale);
		return a.equals(b);
	}
}
