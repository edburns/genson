package com.owlike.genson.functional;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.TransformationException;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;

public class GenericTypesTest {
	private Genson genson = new Genson();

	@Test
	public void testSerializeWithGenericType() throws TransformationException, IOException {
		ContainerClass cc = new ContainerClass();
		cc.urlContainer = new MyGenericClass<URL>();
		cc.urlContainer.tField = new URL("http://www.google.com");

		assertEquals("{\"urlContainer\":{\"tField\":\"http://www.google.com\"}}",
				genson.serialize(cc));
	}

	@Test
	public void testDeserializeWithGenericType() throws TransformationException, IOException {
		ContainerClass cc = genson.deserialize(
				"{\"urlContainer\":{\"tField\":\"http://www.google.com\"}}", ContainerClass.class);
		assertEquals(URL.class, cc.urlContainer.tField.getClass());
		assertEquals(new URL("http://www.google.com"), cc.urlContainer.tField);
	}

	@Test
	public void testDeserializeDeepGenericsUsingGenericType() throws TransformationException,
			IOException {
		Type type = new GenericType<MyGenericClass<ContainerClass>>() {
		}.getType();
		MyGenericClass<ContainerClass> mgc = genson.deserialize(
				"{\"tField\":{\"urlContainer\":{\"tField\":\"http://www.google.com\"}}}", type);
		assertEquals(ContainerClass.class, mgc.tField.getClass());
		assertEquals(URL.class, mgc.tField.urlContainer.tField.getClass());
		assertEquals(new URL("http://www.google.com"), mgc.tField.urlContainer.tField);
	}

	@Test
	public void testMultiLevelGenerics() throws TransformationException, IOException {
		MyContainerPojo pojo = new MyContainerPojo();
		pojo.a = new MyContainer<GenericTypesTest.MyGenericClass<URL>, URL>();
		pojo.a.e = new URL("http://google.com");
		pojo.a.t = new MyGenericClass<URL>();
		pojo.a.t.tField = new URL("http://google.com");

		String json = genson.serialize(pojo);
		MyContainerPojo pojoDeserialized = genson.deserialize(json, MyContainerPojo.class);

		assertEquals(pojo.a.e, pojoDeserialized.a.e);
		assertEquals(pojo.a.t.tField, pojoDeserialized.a.t.tField);
	}

	@Test
	public void testMultiLevelGenericsWithCustomConverter() throws TransformationException,
			IOException {
		MyContainerPojo pojo = new MyContainerPojo();
		pojo.a = new MyContainer<GenericTypesTest.MyGenericClass<URL>, URL>();
		pojo.a.e = new URL("http://google.com");
		pojo.a.t = new MyGenericClass<URL>();
		pojo.a.t.tField = new URL("http://google.com");

		Genson genson = new Genson.Builder().withConverters(new MyGenericClassUrlConverter())
				.create();

		MyGenericClassUrlConverter.used = false;
		String json = genson.serialize(pojo);
		assertTrue(MyGenericClassUrlConverter.used);
		MyGenericClassUrlConverter.used = false;
		MyContainerPojo pojoDeserialized = genson.deserialize(json, MyContainerPojo.class);
		assertTrue(MyGenericClassUrlConverter.used);
		assertEquals(pojo.a.e, pojoDeserialized.a.e);
		assertEquals(pojo.a.t.tField, pojoDeserialized.a.t.tField);
	}

	@Test
	public void testDeepGenericsWithDefaultCollectionConverters() throws TransformationException,
			IOException {
		MyClass mc = new MyClass();
		// we use URL instead of a plain string or a primitive type, because genson can guess them
		// so using a complex object can not be tricked
		mc.mapOfSets = new HashMap<String, Set<URL>>();
		Set<URL> set0 = new HashSet<URL>();
		set0.add(new URL("http://google.com"));
		Set<URL> set1 = new HashSet<URL>();
		set1.add(new URL("http://google.com"));
		set1.add(new URL("http://twitter.com"));
		Set<URL> set2 = new HashSet<URL>();
		set2.add(new URL("http://google.com"));
		set2.add(new URL("http://twitter.com"));
		set2.add(new URL("http://facebook.com"));
		mc.mapOfSets.put("set0", set0);
		mc.mapOfSets.put("set1", set1);
		mc.mapOfSets.put("set2", set2);

		String json = genson.serialize(mc);
		MyClass mcDeserialized = genson.deserialize(json, MyClass.class);

		assertEquals(mc.mapOfSets.size(), mcDeserialized.mapOfSets.size());
		for (Map.Entry<String, Set<URL>> e : mc.mapOfSets.entrySet()) {
			assertNotNull(mcDeserialized.mapOfSets.get(e.getKey()));
			assertEqualSets(e.getValue(), mcDeserialized.mapOfSets.get(e.getKey()));
		}
	}

	static <T> void assertEqualSets(Set<T> expectedSet, Set<T> set) {
		assertEquals(expectedSet.size(), set.size());
		for (Object expected : expectedSet)
			set.contains(expected);
	}

	static class MyGenericClassUrlConverter implements Converter<MyGenericClass<URL>> {
		static boolean used;

		@Override
		public void serialize(MyGenericClass<URL> object, ObjectWriter writer, Context ctx)
				throws TransformationException, IOException {
			used = true;
			writer.beginObject().writeName("tField").writeValue(object.tField.toString())
					.endObject();
		}

		@Override
		public MyGenericClass<URL> deserialize(ObjectReader reader, Context ctx)
				throws TransformationException, IOException {
			used = true;
			MyGenericClass<URL> mgc = new MyGenericClass<URL>();
			reader.beginObject();
			reader.next();
			mgc.tField = new URL(reader.valueAsString());
			reader.endObject();
			return mgc;
		}
	}

	static class ContainerClass {
		MyGenericClass<URL> urlContainer;
	}

	static class MyGenericClass<T> {
		T tField;
	}

	static class TV extends MyGenericClass<URL> {

	}

	static class MyContainerPojo {
		public MyContainer<MyGenericClass<URL>, URL> a;
	}

	static class MyContainer<T, E> {
		public T t;
		public E e;
	}

	static class MyClass {
		public Map<String, Set<URL>> mapOfSets;
	}
}
