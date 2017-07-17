package alice.tuprolog;

import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Ignore
public class JavaLibraryTestCase extends TestCase {
	String theory;
	Prolog engine = new Prolog();
	Solution info;
	String result;
	String paths;
	
	public void testGetPrimitives() {
		Library library = new OOLibrary();
		Map<Integer, List<PrimitiveInfo>> primitives = library.getPrimitives();
		Assert.assertEquals(3, primitives.size());
		Assert.assertEquals(0, primitives.get(PrimitiveInfo.DIRECTIVE).size());
		Assert.assertTrue(primitives.get(PrimitiveInfo.PREDICATE).size() > 0);
		Assert.assertEquals(0, primitives.get(PrimitiveInfo.FUNCTOR).size());
	}

	public void testAnonymousObjectRegistration() throws InvalidTheoryException, InvalidObjectIdException {	
		OOLibrary lib = (OOLibrary) engine.library("alice.tuprolog.lib.OOLibrary");
		String theory = "demo(X) :- X <- update. \n";
		engine.setTheory(new Theory(theory));
		TestCounter counter = new TestCounter();
		// check registering behaviour
		Struct t = lib.register(counter);
		engine.solve(new Struct("demo", t));
		Assert.assertEquals(1, counter.getValue());
		// check unregistering behaviour
		lib.unregister(t);
		Solution goal = engine.solve(new Struct("demo", t));
		Assert.assertFalse(goal.isSuccess());
	}

	public void testDynamicObjectsRetrival() throws PrologException {
		Prolog engine = new Prolog();
		OOLibrary lib = (OOLibrary) engine.library("alice.tuprolog.lib.OOLibrary");
		String theory = "demo(C) :- \n" +
				"java_object('alice.tuprolog.TestCounter', [], C), \n" +
				"C <- update, \n" +
				"C <- update. \n";			
		engine.setTheory(new Theory(theory));
		Solution info = engine.solve("demo(Obj).");
		Struct id = (Struct) info.getVarValue("Obj");
		TestCounter counter = (TestCounter) lib.getRegisteredDynamicObject(id);
		Assert.assertEquals(2, counter.getValue());
	}

	
	public void test_java_object() throws PrologException, IOException
	{
		// Testing URLClassLoader with a paths' array
		setPath(true);
		theory = "demo(C) :- \n" +
				"set_classpath([" + paths + "]), \n" +
				"java_object('Counter', [], Obj), \n" +
				"Obj <- inc, \n" +
				"Obj <- inc, \n" +
				"Obj <- getValue returns C.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
		alice.tuprolog.Number result2 = (alice.tuprolog.Number) info.getVarValue("Value");
		Assert.assertEquals(2, result2.intValue());

		// Testing URLClassLoader with java.lang.String class
		theory = 	"demo_string(S) :- \n" +
				"java_object('java.lang.String', ['MyString'], Obj_str), \n" +
				"Obj_str <- toString returns S.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo_string(StringValue).");
        assertTrue(info.isSuccess());
		result = info.getVarValue("StringValue").toString().replace("'", "");
		Assert.assertEquals("MyString", result);
	}
	

	public void test_java_object_2() throws PrologException, IOException
	{
		setPath(true);
		theory = "demo_hierarchy(Gear) :- \n"
					+ "set_classpath([" + paths + "]), \n" 
					+ "java_object('Bicycle', [3, 4, 5], MyBicycle), \n"
					+ "java_object('MountainBike', [5, 6, 7, 8], MyMountainBike), \n"
					+ "MyMountainBike <- getGear returns Gear.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo_hierarchy(Res).");
        assertFalse(info.isHalted());
		alice.tuprolog.Number result2 = (alice.tuprolog.Number) info.getVarValue("Res");
		Assert.assertEquals(8, result2.intValue());
	}
	
	public void test_invalid_path_java_object() throws PrologException, IOException
	{
		//Testing incorrect path
		setPath(false);
		theory = "demo(Res) :- \n" +
				"set_classpath([" + paths + "]), \n" + 
				"java_object('Counter', [], Obj_inc), \n" +
				"Obj_inc <- inc, \n" +
				"Obj_inc <- inc, \n" +
				"Obj_inc <- getValue returns Res.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isHalted());
	}

	public void test_java_call_3() throws PrologException, IOException
	{
		//Testing java_call_3 using URLClassLoader 
		setPath(true); 
		theory = "demo(Value) :- set_classpath([" + paths + "]), class('TestStaticClass') <- echo('Message') returns Value.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(StringValue).");
        assertTrue(info.isSuccess());
		result = info.getVarValue("StringValue").toString().replace("'", "");
		Assert.assertEquals("Message", result);

		//Testing get/set static Field 
		setPath(true);
		theory = "demo_2(Value) :- set_classpath([" + paths + "]), class('TestStaticClass').'id' <- get(Value).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo_2(Res).");
        assertTrue(info.isSuccess());
		Assert.assertEquals(0, Integer.parseInt(info.getVarValue("Res").toString()));
		
		theory = "demo_2(Value, NewValue) :- set_classpath([" + paths + "]), class('TestStaticClass').'id' <- set(Value), \n" +
				"class('TestStaticClass').'id' <- get(NewValue).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo_2(5, Val).");
        assertTrue(info.isSuccess());
		Assert.assertEquals(5, Integer.parseInt(info.getVarValue("Val").toString()));
		
	}

	public void test_invalid_path_java_call_4() throws PrologException, IOException
	{
		//Testing java_call_4 with invalid path
		setPath(false);
		theory = "demo(Value) :- set_classpath([" + paths + "]), class('TestStaticClass') <- echo('Message') returns Value.";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(StringValue).");
        assertTrue(info.isHalted());
	}

	public void test_java_array() throws PrologException, IOException
	{
		//Testing java_array_length using URLClassLoader 
		setPath(true);
		theory =  "demo(Size) :- set_classpath([" + paths + "]), java_object('Counter', [], MyCounter), \n"
				+ "java_object('Counter[]', [10], ArrayCounters), \n"
				+ "java_array_length(ArrayCounters, Size).";

		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
		alice.tuprolog.Number resultInt = (alice.tuprolog.Number) info.getVarValue("Value");
		Assert.assertEquals(10, resultInt.intValue());

		//Testing java_array_set and java_array_get
		setPath(true);
		theory =  "demo(Res) :- set_classpath([" + paths + "]), java_object('Counter', [], MyCounter), \n"
				+ "java_object('Counter[]', [10], ArrayCounters), \n"
				+ "MyCounter <- inc, \n"
				+ "java_array_set(ArrayCounters, 0, MyCounter), \n"
				+ "java_array_get(ArrayCounters, 0, C), \n"
				+ "C <- getValue returns Res.";

		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
		alice.tuprolog.Number resultInt2 = (alice.tuprolog.Number) info.getVarValue("Value");
		Assert.assertEquals(1, resultInt2.intValue());
	}

	public void test_set_classpath() throws PrologException, IOException
	{
		//Testing java_array_length using URLClassLoader 
		setPath(true);
		
		theory =  "demo(Size) :- set_classpath([" + paths + "]), \n "
				+ "java_object('Counter', [], MyCounter), \n"
				+ "java_object('Counter[]', [10], ArrayCounters), \n"
				+ "java_array_length(ArrayCounters, Size).";

		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
		alice.tuprolog.Number resultInt = (alice.tuprolog.Number) info.getVarValue("Value");
		Assert.assertEquals(10, resultInt.intValue());
	}
	
	public void test_get_classpath() throws PrologException, IOException
	{
		//Testing get_classpath using DynamicURLClassLoader with not URLs added
		theory =  "demo(P) :- get_classpath(P).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
        assertTrue(info.getTerm("Value").isList());
		Assert.assertEquals("[]", info.getTerm("Value").toString());

		//Testing get_classpath using DynamicURLClassLoader with not URLs added
		setPath(true);

		theory =  "demo(P) :- set_classpath([" + paths + "]), get_classpath(P).";

		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Value).");
        assertTrue(info.isSuccess());
        assertTrue(info.getTerm("Value").isList());
		Assert.assertEquals("[" + paths + "]", info.getTerm("Value").toString());
		
//		// Test if get_classpath(PathList) unifies with the DynamicURLClassLoader urls
//		theory =  "demo(P) :- set_classpath([" + paths + "]), get_classpath([" + paths + "]).";
//		
//		engine.setTheory(new Theory(theory));
//		info = engine.solve("demo(S).");
//		assertEquals(true, info.isSuccess());
	}
	
	public void test_register_1() throws PrologException, IOException
	{
		setPath(true);
		theory = "demo(Obj) :- \n" +
				"set_classpath([" + paths + "]), \n" +
				"java_object('Counter', [], Obj), \n" +
				"Obj <- inc, \n" +
				"Obj <- inc, \n" +
				"register(Obj).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(R).");
        assertTrue(info.isSuccess());
		
		theory = "demo2(Obj, Val) :- \n"
				+ "Obj <- inc, \n"
				+ "Obj <- getValue returns Val.";
		engine.addTheory(new Theory(theory));
		String obj =  info.getTerm("R").toString();
		Solution info2 = engine.solve("demo2(" + obj + ", V).");
        assertTrue(info2.isSuccess());
		Assert.assertEquals(3, Integer.parseInt(info2.getVarValue("V").toString()));
	
		// Test invalid object_id registration
		theory = "demo(Obj1) :- register(Obj1).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Res).");
        assertTrue(info.isHalted());
	}
	
	
	public void test_unregister_1() throws PrologException, IOException
	{
		// Test invalid object_id unregistration
		theory = "demo(Obj1) :- unregister(Obj1).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Res).");
        assertTrue(info.isHalted());
		
		setPath(true);
		theory = "demo(Obj) :- \n" +
				"set_classpath([" + paths + "]), \n" +
				"java_object('Counter', [], Obj), \n" +
				"Obj <- inc, \n" +
				"Obj <- inc, \n" +
				"register(Obj), unregister(Obj).";
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(Res).");
        assertTrue(info.isSuccess());
		OOLibrary lib = (OOLibrary) engine.library("alice.tuprolog.lib.OOLibrary");
		Struct id = (Struct) info.getTerm("Res");
		Object obj = lib.getRegisteredObject(id);
		Assert.assertNull(obj);
	}
	
	public void test_java_catch() throws PrologException, IOException
	{
		setPath(true);
		theory = "goal :- set_classpath([" + paths + "]), java_object('TestStaticClass', [], Obj), Obj <- testMyException. \n"
				+"demo(StackTrace) :- java_catch(goal, [('java.lang.IllegalArgumentException'( \n"
						+ "Cause, Msg, StackTrace),write(Msg))], \n"
						+ "true).";
				
		engine.setTheory(new Theory(theory));
		info = engine.solve("demo(S).");
        assertTrue(info.isSuccess());
	}
	
	public void test_interface() throws PrologException, IOException
	{
		setPath(true);
		theory = "goal1 :- set_classpath([" + paths + "])," +
				"java_object('Pippo', [], Obj), class('Pluto') <- method(Obj).";
				
		engine.setTheory(new Theory(theory));
		info = engine.solve("goal1.");
        assertTrue(info.isSuccess());
		
		theory = "goal2 :- set_classpath([" + paths + "])," +
				"java_object('Pippo', [], Obj), class('Pluto') <- method2(Obj).";
				
		engine.setTheory(new Theory(theory));
		info = engine.solve("goal2.");
        assertTrue(info.isSuccess());
		
		theory = "goal3 :- java_object('Pippo', [], Obj), set_classpath([" + paths + "]), class('Pluto') <- method(Obj).";
				
		engine.setTheory(new Theory(theory));
		info = engine.solve("goal3.");
        assertTrue(info.isSuccess());
		
		theory = "goal4 :- set_classpath([" + paths + "]), " +
					"java_object('IPippo[]', [5], Array), " +
					"java_object('Pippo', [], Obj), " +
					"java_array_set(Array, 0, Obj)," +
					"java_array_get(Array, 0, Obj2)," +
					"Obj2 <- met.";
		
		engine.setTheory(new Theory(theory));
		info = engine.solve("goal4.");
        assertTrue(info.isSuccess());
		
		theory = "goal5 :- set_classpath([" + paths + "])," +
				"java_object('Pippo', [], Obj)," +
				"class('Pluto') <- method(Obj as 'IPippo').";
		
		engine.setTheory(new Theory(theory));
		info = engine.solve("goal5.");
        assertTrue(info.isSuccess());
		
	}
	
	/**
	 * @param valid: used to change a valid/invalid array of paths
	 */
	private void setPath(boolean valid) throws IOException
	{
		File file = new File(".");
		
		// Array paths contains a valid path
		if(valid)
		{
			paths = "'" + file.getCanonicalPath() + "'," +
					"'" + file.getCanonicalPath() 
					+ File.separator + "test"
					+ File.separator + "unit" 
					+ File.separator + "TestURLClassLoader.jar'";
			paths += "," +	"'" + file.getCanonicalPath() 
					+ File.separator + "test"
					+ File.separator + "unit" 
					+ File.separator + "TestInterfaces.jar'";
		}
		// Array paths does not contain a valid path
		else
		{
			paths = "'" + file.getCanonicalPath() + "'";
		}
	}
}
