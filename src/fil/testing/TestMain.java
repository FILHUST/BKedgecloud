package fil.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
////////////////////////////////////////////////////////////////////////
//		Map<Integer, ArrayList<TestObject>> map = new HashMap<>();
//		for(int i = 0; i < 10; i++) {
//			ArrayList<TestObject> array01 = new ArrayList<>();
//			for(int j = 0; j < 5; j++) {
//				TestObject object = new TestObject();
//				array01.add(object);
//			}
//			map.put(i, array01);
//		}
//		System.out.println("Array 01: "+ map.get(0));
//		ArrayList<TestObject> array02 = map.get(0);
//		array02.clear();
//		System.out.println("Array 02: "+ map.get(0));
/////////////////////////////////////////////////////////////////////////
		ArrayList<TestObject> abc = new ArrayList<>();
		TestMain test01 = new TestMain();
		test01.testReference(abc);
		System.out.println("Size: "+ abc.size());
		for(int i = 0; i < abc.size(); i++) {
			System.out.println("Value: "+ abc.get(i).isPosition());
			System.out.println("Object after: "+ abc.get(i));
		}
	}
	public void testReference(ArrayList<TestObject> abc) {
		for(int i = 0; i < 3; i++) {
			TestObject object = new TestObject();
			object.setPosition(false);
			abc.add(object);
			System.out.println("Object b4: "+ object);
		}
	}

}
