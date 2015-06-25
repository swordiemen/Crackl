package analysis;

public class Pointer extends Type{
	private int size;
	private Type type;
	
	public Pointer(Types t) {
		this(new Type(t));
	}
	
	public Pointer(Type t){
		super(Types.Pointer);
		this.type = t;
		this.size = Integer.SIZE/Byte.SIZE;
	}
	
	@Override
	public int getSize(){
		return size;
	}
	
	public void setSize(int s){
		size = s;
	}
	
	@Override
	public Type getTypeObj(){
		return type;
	}
	
	@Override
	public Types getType(){
		return Types.Pointer;
	}
	
	@Override
	public boolean isPointer(){
		return true;
	}
}
