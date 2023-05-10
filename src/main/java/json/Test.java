package json;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import json.xjson.XJSON;

public class Test {
	
	public static void xjsonTest() {
		TestObj obj = new TestObj(true);
		try {
			String json = XJSON.stringify(obj);
			System.out.println("********");
			System.out.println(json);
			System.out.println("********");
			
			TestObj p = XJSON.parse(json,  TestObj.class);
			System.out.println(p.ret);
			System.out.println(p.c);
			System.out.println(p.b);
			System.out.println(p.code);
			System.out.println(p.t);
			System.out.println(p.msg);
			System.out.println(p.bigB.doubleValue());
			Queue<TestObj> qu = p.list.get(0);
			for(TestObj o: qu) {
				System.out.println(o.ret);
				System.out.println(p.c);
				System.out.println(p.b);
				System.out.println(o.code);
				System.out.println(o.t);
				System.out.println(o.msg);
				System.out.println(o.bigB.doubleValue());
			}

			System.out.println("********");
			XJSON.useBeautifyMode(false);
			String str = XJSON.stringify(p);
			System.out.println(str);
			System.out.println("********");
			

			System.out.println("********");
			p = XJSON.parse(str, TestObj.class);
			System.out.println(p.list.get(0).peek().bigB.doubleValue());
			System.out.println("********");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void testArr() {

		TestObj obj = new TestObj(true);
		
		try {
			String json = XJSON.stringify(Arrays.asList(obj));
//			json = json.replace("172714.124", "172 714.124");
			System.out.println(json);
			LinkedList<TestObj> jsonArr = XJSON.parseList(json, TestObj.class);
			TestObj p = jsonArr.get(0);
			System.out.println(p.ret);
			System.out.println(p.code);
			System.out.println(p.t);
			System.out.println(p.msg);
			

			System.out.println("********");
			String str = XJSON.stringify(jsonArr);
			System.out.println(str);
			System.out.println("********");

			System.out.println("********");
			jsonArr = XJSON.parseList(str, TestObj.class);
			System.out.println(jsonArr.get(0).list.get(0).peek().bigB.doubleValue());
			System.out.println("********");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		xjsonTest();
//		testArr();
	}
}
