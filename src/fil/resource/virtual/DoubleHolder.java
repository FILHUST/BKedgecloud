package fil.resource.virtual;

public class DoubleHolder {
	public Double value;
	 
    public DoubleHolder(Double value) {
        this.value = value;
    }
 
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
