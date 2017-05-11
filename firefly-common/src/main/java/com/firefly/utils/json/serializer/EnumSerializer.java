package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class EnumSerializer implements Serializer {
	
	private EnumObj[] enumObjs;
	
	public EnumSerializer(Class<?> clazz) {
		List<EnumObj> list = new ArrayList<EnumObj>();
		Object[] o = clazz.getEnumConstants();
		enumObjs = new EnumObj[o.length];
		for (Object o1 : o) {
			EnumObj enumObj = new EnumObj();
			enumObj.e = o1;
			enumObj.value = (QUOTE + ((Enum<?>)o1).name() + QUOTE).toCharArray();
			list.add(enumObj);
		}
		list.toArray(enumObjs);
	}

	@Override
	public void convertTo(JsonWriter writer, Object obj) throws IOException {
		writer.write(find(obj).value);
	}
	
	private EnumObj find(Object obj) {
		for(EnumObj o : enumObjs) {
			if(o.e == obj)
				return o;
		}
		return null;
	}
	
	private class EnumObj {
		Object e;
		char[] value;
	}

}
