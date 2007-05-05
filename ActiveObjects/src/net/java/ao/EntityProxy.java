/*
 * Created on May 2, 2007
 */
package net.java.ao;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.java.ao.schema.Table;

import static net.java.ao.Utilities.*;

/**
 * @author Daniel Spiewak
 */
public class EntityProxy<T extends Entity> implements InvocationHandler {
	private EntityManager manager;	
	private Class<T> type;
	
	private Map<String, Object> cache;
	
	private int id;

	public EntityProxy(EntityManager manager, Class<T> type) {
		this.manager = manager;
		this.type = type;
		
		cache = new HashMap<String, Object>();
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("setID")) {
			setID((Integer) args[0]);

			return Void.TYPE;
		} else if (method.getName().equals("getID")) {
			return getID();
		} else if (method.getName().equals("hashCode")) {
			return hashCodeImpl();
		} else if (method.getName().equals("equals")) {
			return equalsImpl(proxy, args[0]);
		} else if (method.getName().equals("toString")) {
			return toStringImpl();
		}

		String tableName = convertDowncaseName(convertSimpleClassName(type.getCanonicalName()));
		
		if (type.getAnnotation(Table.class) != null) {
			tableName = type.getAnnotation(Table.class).value();
		}
		
		Mutator mutatorAnnotation = method.getAnnotation(Mutator.class);
		Accessor accessorAnnotation = method.getAnnotation(Accessor.class);
		OneToMany oneToManyAnnotation = method.getAnnotation(OneToMany.class);

		if (mutatorAnnotation != null) {
			invokeSetter(getID(), tableName, mutatorAnnotation.value(), args[0]);
			return Void.TYPE;
		} else if (accessorAnnotation != null) {
			return invokeGetter(getID(), tableName, accessorAnnotation.value(), method.getReturnType());
		} else if (oneToManyAnnotation != null && method.getReturnType().isArray()) {
			Class<?> type = method.getReturnType().getComponentType();
			String otherTableName = convertDowncaseName(convertSimpleClassName(type.getCanonicalName()));
			
			String mapField = oneToManyAnnotation.value();
			if (mapField.equals("")) {
				mapField = tableName + "ID";
			}
			
			return retrieveRelations(otherTableName, mapField, getID(), (Class<? extends Entity>) type);
		} else if (method.getName().startsWith("get")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceIneritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("is")) {
			String name = convertDowncaseName(method.getName().substring(2));
			if (interfaceIneritsFrom(method.getReturnType(), Entity.class)) {
				name += "ID";
			}
			
			return invokeGetter(getID(), tableName, name, method.getReturnType());
		} else if (method.getName().startsWith("set")) {
			String name = convertDowncaseName(method.getName().substring(3));
			if (interfaceIneritsFrom(method.getParameterTypes()[0], Entity.class)) {
				name += "ID";
			}
			invokeSetter(getID(), tableName, name, args[0]);
			
			return Void.TYPE;
		}

		return null;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public int hashCodeImpl() {
		return hashCode();
	}

	public boolean equalsImpl(Object proxy, Object obj) {
		return proxy == obj;
	}

	public String toStringImpl() {
		return "";
	}

	private <V> V invokeGetter(int id, String table, String name, Class<V> type) throws Throwable {
		if (cache.containsKey(name)) {
			return (V) cache.get(name);
		}
		
		V back = null;
		Connection conn = manager.getProvider().getConnection();
		
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT " + name + " FROM " + table + " WHERE id = ?");
			stmt.setInt(1, id);

			ResultSet res = stmt.executeQuery();
			if (res.next()) {
				back = convertValue(res, name, type);
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}

		if (back != null) {
			cache.put(name, back);
		}
		
		return back;
	}

	private void invokeSetter(int id, String table, String name, Object value) throws Throwable {
		cache.put(name, value);
		
		Connection conn = manager.getProvider().getConnection();
		try {
			String sql = "UPDATE " + table + " SET " + name + " = ? WHERE id = ?";

			if (value == null) {
				sql = "UPDATE " + table + " SET " + name + " = NULL WHERE id = ?";
			}

			PreparedStatement stmt = conn.prepareStatement(sql);

			int index = 1;
			if (value != null) {
				convertValue(stmt, index++, value);
			}
			stmt.setInt(index++, id);

			stmt.executeUpdate();

			stmt.close();
		} finally {
			conn.close();
		}
	}
	
	private <V extends Entity> V[] retrieveRelations(String table, String relate, int id, Class<V> type) throws Throwable {
		List<V> back = new ArrayList<V>(); 
		Connection conn = manager.getProvider().getConnection();
		
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM " + table + " WHERE " + relate + " = ?");
			stmt.setInt(1, id);
			
			ResultSet res = stmt.executeQuery();
			while (res.next()) {
				back.add(manager.getEntity(res.getInt("id"), type));
			}
			res.close();
			stmt.close();
		} finally {
			conn.close();
		}
		
		return back.toArray((V[]) Array.newInstance(type, back.size()));
	}

	private <V> V convertValue(ResultSet res, String field, Class<V> type) throws SQLException {
		if (res.getString(field) == null) {
			return null;
		}

		if (type.equals(Integer.class) || type.equals(int.class)) {
			return (V) new Integer(res.getInt(field));
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			return (V) new Long(res.getLong(field));
		} else if (type.equals(Short.class) || type.equals(short.class)) {
			return (V) new Short(res.getShort(field));
		} else if (type.equals(Float.class) || type.equals(float.class)) {
			return (V) new Float(res.getFloat(field));
		} else if (type.equals(Double.class) || type.equals(double.class)) {
			return (V) new Double(res.getDouble(field));
		} else if (type.equals(Byte.class) || type.equals(byte.class)) {
			return (V) new Byte(res.getByte(field));
		} else if (type.equals(String.class)) {
			return (V) res.getString(field);
		} else if (type.equals(Calendar.class)) {
			Calendar back = Calendar.getInstance();
			back.setTimeInMillis(res.getTimestamp(field).getTime());

			return (V) back;
		} else if (type.equals(Date.class)) {
			return (V) new Date(res.getTimestamp(field).getTime());
		} else if (type.equals(URL.class)) {
			return (V) res.getURL(field);
		} else if (type.equals(InputStream.class)) {
			return (V) res.getBlob(field).getBinaryStream();
		} else if (interfaceIneritsFrom(type, Entity.class)) {
			return (V) manager.getEntity(res.getInt(field), (Class<? extends Entity>) type);
		} else {
			throw new RuntimeException("Unrecognized type: " + type.toString());
		}
	}

	private void convertValue(PreparedStatement stmt, int index, Object value) throws SQLException {
		if (value instanceof Integer) {
			stmt.setInt(index, (Integer) value);
		} else if (value instanceof Long) {
			stmt.setLong(index, (Long) value);
		} else if (value instanceof Short) {
			stmt.setShort(index, (Short) value);
		} else if (value instanceof Float) {
			stmt.setFloat(index, (Float) value);
		} else if (value instanceof Double) {
			stmt.setDouble(index, (Double) value);
		} else if (value instanceof Byte) {
			stmt.setByte(index, (Byte) value);
		} else if (value instanceof String) {
			stmt.setString(index, (String) value);
		} else if (value instanceof URL) {
			stmt.setURL(index, (URL) value);
		} else if (value instanceof Calendar) {
			stmt.setTimestamp(index, new Timestamp(((Calendar) value).getTimeInMillis()));
		} else if (value instanceof Date) {
			stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
		} else if (value instanceof Entity) {
			stmt.setInt(index, ((Entity) value).getID());
		} else {
			throw new RuntimeException("Unrecognized type: " + value.getClass().toString());
		}
	}
}