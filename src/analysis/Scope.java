package analysis;

import java.util.HashMap;
import java.util.Map;

public class Scope {
	private int size;
	private Map<String, Type> types;
	private Map<String, Integer> offsets;
	private Scope prevScope;
	
	public Scope(Scope s){
		types = new HashMap<String, Type>();
		offsets = new HashMap<String, Integer>();
		prevScope = s;
	}
	
	public Scope getScope(){
		return prevScope;
	}
	
	public boolean exists(String var){
		return types.containsKey(var);
	}
	
	public boolean put(String var, Type type){
		boolean isDecl = exists(var);
		if(!isDecl){
			types.put(var, type);
			offsets.put(var, size);
			size += type.getSize();
		}
		return isDecl;
	}
	
	public int getBaseAddress(){
		if(prevScope == null){
			return 0;
		}
		return prevScope.getSize() + prevScope.getBaseAddress();
	}
	
	private int getSize() {
		return size;
	}

	public Type getType(String var){
		Type type = types.get(var);
//		if(type != null){
//			return type;
//		}else if(prevScope == null && type == null){
//			return null;
//		}else{
//			return prevScope.getType(var)
//		}
		return type;
	}
	
	public int getOffset(String var){
		return offsets.get(var);
	}
}
