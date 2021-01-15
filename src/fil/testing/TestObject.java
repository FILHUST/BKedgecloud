package fil.testing;

public class TestObject {
	private int abc;
	private boolean position;
	public TestObject() {
		setAbc(1);
		setPosition(true);
	}
	public int getAbc() {
		return abc;
	}
	public void setAbc(int abc) {
		this.abc = abc;
	}
	public boolean isPosition() {
		return position;
	}
	public void setPosition(boolean position) {
		this.position = position;
	}
}
