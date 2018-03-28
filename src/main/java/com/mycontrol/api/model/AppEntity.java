package com.mycontrol.api.model;

import com.mycontrol.api.utils.ReflectionUtils;

public class AppEntity {
	
	@Override
	public boolean equals(Object obj)
	{
		try
		{
			Object idThis = ReflectionUtils.getIdValue(this);
			Object idObj = ReflectionUtils.getIdValue(obj);
			return ReflectionUtils.equals(idThis, idObj);
		} catch(Exception e)
		{
			System.out.println(e);
		}
		return super.equals(obj);
	}

}
