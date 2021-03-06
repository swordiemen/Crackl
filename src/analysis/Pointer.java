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
		//return size;
		return 1;
	}
	
	public void setSize(int s){
		size = s;
	}
	
	@Override
	public boolean equals(Object obj){
		return ((Type) obj).isPointer() &&
				((Pointer) obj).getTypeObj().equals(getTypeObj());
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
	
	@Override
	public String toString(){
		return "@" + type.toString();
	}
}
