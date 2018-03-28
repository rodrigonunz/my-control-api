package com.mycontrol.api.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Ignore;
import org.springframework.util.CollectionUtils;

import com.mycontrol.api.model.AppEntity;

public class ReflectionUtils {
	/**
	 * Retorna uma lista de Id's extraidos de uma lista
	 * 
	 * @param l
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static List getIdList(List l) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		List list = new ArrayList<>();
		if(CollectionUtils.isEmpty(l))
			return list;
		Class clazz = l.get(0).getClass();
		String idName = getIdName(clazz);

		for(Object o : l)
		{
			Field id = o.getClass().getDeclaredField(idName);
			id.setAccessible(true);
			list.add(id.get(o));
		}

		return list;
	}

	/**
	 * Monta uma String unica com nome e valor de todos os atributos de uma classe.<br/>
	 * Caso deseja ignorar algum atributo, usar a anotacao {@link Ignore} (ver {@link ReflectionUtils#fieldIsUsed()})<br/>
	 * <br/>
	 * 22/06/2016: Ignora campos nulos (somente alteracao visual)
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static String attributes(Object o, boolean breakLine) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		StringBuilder sb = new StringBuilder();
		String breakString = breakLine ? "\n" : "; ";

		Field[] fields = o.getClass().getDeclaredFields();
		for(Field field : fields)
		{
			field.setAccessible(true);
			Object f = field.get(o);
			if(f == null)
				continue;

			if(f instanceof Collection)
			{
				sb.append(field.getName()).append(": ").append(((Collection) f).size()).append(breakString);
				continue;
			}
			if(f instanceof AppEntity)
			{
				sb.append(field.getName()).append(": ").append(getIdValue(f)).append(breakString);
				continue;
			}
			String value = castAsString(f);
			if(!mustIgnore(field, value))
				sb.append(field.getName()).append(": ").append(value).append(breakString);
		}

		return sb.toString();
	}

	/**
	 * Monta uma String unica com nome e valor de todos os atributos de uma classe.<br/>
	 * Caso deseja ignorar algum atributo, usar a anotacao {@link Ignore} (ver {@link ReflectionUtils#fieldIsUsed()})<br/>
	 * Exception safe
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static String attributesQuietly(Object o)
	{
		try
		{
			return attributes(o, true);
		} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Extrai os atributos de um objeto e retorna como um Map<br/>
	 * Caso deseja ignorar algum atributo, usar a anotacao {@link Ignore} (ver {@link ReflectionUtils#fieldIsUsed()})<br/>
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static Map<String, Object> attributesMap(Object o, boolean ignoreNull)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Map<String, Object> attributes = new HashMap<String, Object>();

		Field[] fields = o.getClass().getDeclaredFields();
		for(Field field : fields)
		{
			field.setAccessible(true);
			Object value = field.get(o);
			if(ignoreNull && value == null)
				continue;

			if(!mustIgnore(field, value))
				attributes.put(field.getName(), value);
		}

		return attributes;
	}

	/**
	 * A partir de 2 objetos de uma mesma classe, este metodo retorna uma String contendo atributos e valores
	 * somente de campos que possuem valores diferentes (sofreram alteracao de valor).
	 * 
	 * @param newO
	 * @param oldO
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchFieldException
	 */
	public static String getDifference(Object newO, Object oldO) throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException
	{
		StringBuilder sb = new StringBuilder();
		Field[] fields = newO.getClass().getDeclaredFields();

		for(Field newField : fields)
		{
			newField.setAccessible(true);
			Object newResult = newField.get(newO);
			String name = newField.getName();
			Field oldField = oldO.getClass().getDeclaredField(name);
			oldField.setAccessible(true);
			Object oldResult = oldField.get(oldO);
			sb.append(buildDifference(newField, newResult, oldResult));
		}

		return sb.toString();
	}

	/**
	 * Realiza a comparacao de um atributo de 2 objetos de mesma classe
	 * 
	 * @param field
	 * @param newResult
	 * @param oldResult
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private static String buildDifference(Field field, Object newResult, Object oldResult)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException
	{
		if(newResult instanceof Collection)
			return buildDifferenceCollection(field.getName(), newResult, oldResult);

		if(newResult instanceof AppEntity)
			return buildDifferenceScpiEntity(field.getName(), newResult, oldResult);

		/*if(newResult instanceof ScpiClone)
			return buildDifferenceScpiClone(field.getName(), newResult, oldResult);*/

		if(!mustIgnore(field, newResult))
			return buildDifferencePrimitive(field.getName(), newResult, oldResult);

		return "";
	}

	/**
	 * Realiza a comparacao de um atributo de 2 objetos de mesma classe caso o atributo for instancia de {@link ScpiClone}
	 * 
	 * @param name
	 * @param newResult
	 * @param oldResult
	 * @return
	 */
	private static String buildDifferenceScpiClone(String name, Object newResult, Object oldResult)
	{
		if(!equals(newResult, oldResult))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(": ").append(oldResult).append(" -> ").append(newResult).append("\n");
			return sb.toString();
		}
		return "";
	}

	/**
	 * Realiza a comparacao de um atributo de 2 objetos de mesma classe caso o atributo seja primitivo
	 * 
	 * @param name
	 * @param newResult
	 * @param oldResult
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private static String buildDifferencePrimitive(String name, Object newResult, Object oldResult)
			throws NoSuchFieldException, IllegalAccessException
	{
		if(!equals(newResult, oldResult))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(": ").append(castAsString(oldResult)).append(" -> ").append(castAsString(newResult)).append("\n");
			return sb.toString();
		}
		return "";
	}

	/**
	 * Realiza a comparacao de um atributo de 2 objetos de mesma classe caso o atributo for instancia de {@link ScpiEntity}
	 * 
	 * @param name
	 * @param newResult
	 * @param oldResult
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private static String buildDifferenceScpiEntity(String name, Object newResult, Object oldResult)
			throws NoSuchFieldException, IllegalAccessException
	{
		if(!Objects.equals(newResult, oldResult))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(": ").append(getIdValue(oldResult)).append(" -> ").append(getIdValue(newResult)).append("\n");
			return sb.toString();
		}

		return "";
	}

	/**
	 * Realiza a comparacao de um atributo de 2 objetos de mesma classe caso o atributo for instancia de {@link Collection}
	 * 
	 * @param name
	 * @param newResult
	 * @param oldResult
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private static String buildDifferenceCollection(String name, Object newResult, Object oldResult)
			throws NoSuchFieldException, IllegalAccessException
	{
		Collection newCollection = (Collection) newResult;
		Collection oldCollection = (Collection) oldResult;
		if(!Objects.equals(newResult, oldResult))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(": [").append(oldCollection.size()).append("] -> [").append(newCollection.size()).append("]\n");
			return sb.toString();
		}
		return "";
	}

	/**
	 * Lista as possibilidades para ignorar o campo. Um campo é ignorado se:
	 * <pre>
	 * - é o atributo de serialização (serialVersionUID)
	 * - possui anotação {@link Ignore}
	 * - não for um tipo válido (olhar {@link ReflectionUtils#isValidType()})
	 * </pre>
	 * Obs: não verifica se o valor é nulo
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	private static boolean mustIgnore(Field field, Object value)
	{
		return "serialVersionUID".equals(field.getName()) || !isValidType(value) || !fieldIsUsed(field);
	}

	/**
	 * Força diversos tipos de verificação para saber se os objetos parametrizados são iguais. É Null safe.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equals(Object a, Object b)
	{
		if((a != b) && (a == null || b == null))
			return false;

		boolean equals = false;
		equals |= (a == null && b == null);
		equals |= Objects.equals(a, b);

		if(!equals && (a instanceof Number))
			equals |= MathUtils.equals(new BigDecimal(a.toString()), new BigDecimal(b.toString()));

		return equals;
	}

	/**
	 * Força o objeto fazer cast para String. É null safe.
	 * 
	 * @param o
	 * @return
	 */
	public static String castAsString(Object o)
	{
		if(canUse(o))
		{
			return o.toString();
		}
		return null;
	}

	/**
	 * Valida se o objeto pode ser usado.
	 * 
	 * @param o
	 * @return
	 */
	public static boolean canUse(Object o)
	{
		return o != null && isRawType(o.getClass());
	}

	/**
	 * Valida se o tipo do objeto é válido. É válido se for:
	 * <pre>
	 * - primitivo (int, short, long, boolean ...)
	 * - {@link String}
	 * - {@link Number}
	 * - {@link Boolean}
	 * - {@link Date}
	 * </pre>
	 * 
	 * @param o
	 * @return
	 */
	private static boolean isValidType(Object o)
	{
		return o == null || isRawType(o.getClass());
	}

	private static boolean isRawType(Class c)
	{
		return c.isPrimitive() || String.class.isAssignableFrom(c) || Number.class.isAssignableFrom(c) || Boolean.class.isAssignableFrom(c)
				|| Date.class.isAssignableFrom(c);
	}

	/**
	 * Valida se o valor do objeto é válido.
	 * <pre>
	 * 	- isValidValue(null)     = false
	 * - {@link String}
	 * 	- isValidValue("")       = false
	 * 	- isValidValue("   ")    = false
	 * 	- isValidValue("a")      = true
	 * - {@link Number}
	 * 	- isValidValue(0)        = false
	 * 	- isValidValue(0.0000)   = false
	 * 	- isValidValue(0.001)    = false ({@link BigDecimal#ROUND_HALF_UP}
	 * 	- isValidValue(0.5)      = true  ({@link BigDecimal#ROUND_HALF_UP}
	 * 	- isValidValue(15)       = true
	 * 	- isValidValue(15.2517L) = true
	 * - {@link Collection}
	 * 	- collection isEmpty     = false
	 * 	- collection isNotEmpty  = true
	 * - {@link Boolean} - tudo é valido com {@link Boolean}, só valida se não é null
	 * - {@link Date} - tudo é valido com {@link Date}, só valida se não é null
	 * </pre>
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isValidValue(Object value)
	{
		if(value == null)
			return false;
		if(value instanceof String)
			return StringUtils.isNotBlank((String) value);
		if(value instanceof Number)
			return 0 != ((Number) value).intValue();
		if(value instanceof Collection)
			return !((Collection) value).isEmpty();
		return true;
	}

	/**
	 * Obtém o nome da tabela que a Entity mapeia. Verifica tanto anotações {@link Table} quanto {@link Entity}
	 * 
	 * @param o
	 * @return
	 */
	public static String getTableName(Class o)
	{
		if(o == null)
			return null;

		String tableName = "";
		Annotation annotation = o.getAnnotation(Table.class);
		if(annotation != null)
		{
			Table table = (Table) annotation;
			if(StringUtils.isNotBlank(table.name()))
				tableName = table.name();
		} else
		{
			if(o.isAnnotationPresent(Entity.class))
			{
				tableName = o.getSimpleName();
			}
		}
		return tableName.toUpperCase();
	}

	/**
	 * Obtém o valor do id da entity parametrizada
	 * 
	 * @param o
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object getIdValue(Object o) throws IllegalArgumentException, IllegalAccessException
	{
		if(o != null)
		{
			for(Field field : o.getClass().getDeclaredFields())
			{
				if(field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class))
				{
					field.setAccessible(true);
					return field.get(o);
				}
			}
		}
		return null;
	}

	/**
	 * Obtém o valor do id expresso em String. Caso for uma Pk composta, obtém o valor de todos os atributos.
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static String getIdAsText(Object o) throws IllegalAccessException, InvocationTargetException
	{
		Object idValue = ReflectionUtils.getIdValue(o);
		return idValue instanceof AppEntity ? ReflectionUtils.attributes(idValue, false) : idValue.toString();
	}

	/**
	 * Obtém o nome do id da entity. Verifica tanto ids primitivos quanto composto.
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getIdName(Class clazz)
	{
		for(Field field : clazz.getDeclaredFields())
		{
			if((field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) && fieldIsUsed(field))
				return field.getName();
		}
		return null;
	}

	/**
	 * Obtém o primeiro campo encontrado com anotação parametrizada
	 * 
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	public static Field getFieldByAnnotation(Class clazz, Class annotationClass)
	{
		if(clazz == null)
			return null;
		for(Field field : clazz.getDeclaredFields())
		{
			if(field.getAnnotation(annotationClass) != null)
			{
				return field;
			}
		}
		return getFieldByAnnotation(clazz.getSuperclass(), annotationClass);
	}

	/**
	 * Verifica se o atributo possui anotação {@link Ignore}
	 * 
	 * @param field
	 * @return
	 */
	public static boolean fieldIsUsed(Field field)
	{
		return !field.isAnnotationPresent(Ignore.class);
	}

	/**
	 * Procura um método na classe com o nome parametrizado
	 * 
	 * @param service
	 * @param methodName
	 * @return
	 */
	public static Method scanMethod(Class service, String methodName)
	{
		if(service != null && methodName != null)
		{
			for(Method method : service.getMethods())
			{
				if(StringUtils.equals(method.getName(), methodName))
					return method;
			}
		}
		return null;
	}

	/**
	 * Realiza uma busca em profundidade procurando pela anotação parametrizada no método escolhido
	 * 
	 * @param clazz
	 * @param methodName
	 * @param annotationToFind
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Annotation scanMethodForAnnotation(Class clazz, String methodName, Class annotationToFind)
			throws NoSuchMethodException, SecurityException
	{
		if(clazz == null || StringUtils.isBlank(methodName) || annotationToFind == null)
			return null;
		Method method = ReflectionUtils.scanMethod(clazz, methodName);
		if(method == null)
			return null;
		Annotation annotation = method.getAnnotation(annotationToFind);
		if(annotation == null)
			return scanMethodForAnnotation(clazz.getSuperclass(), methodName, annotationToFind);
		return annotation;
	}

	/**
	 * Obtém o valor do campo
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 */
	public static Object getFieldValue(Object object, String fieldName)
	{
		try
		{
			if(object == null)
				return null;
			Class clazz = object.getClass();

			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(object);
		} catch(Exception e)
		{
			e.printStackTrace();
			//			LOG.error(e);
			return null;
		}
	}

	/**
	 * Inicializa a Entity e todas as entities atributos.
	 * Cuidado com mapeamentos redundantes, provavelmente terá problema de StackOverflow.<br/>
	 * 
	 * Se usou este método para inicializar, utilize {@link ReflectionUtils#removeEmpty(Object)} antes de persistir.
	 * <br/><br/>
	 * 
	 * Para mapeamentos redundantes, coloque a anotação {@link Ignore} no campo que ocorre redundância.
	 * Ele não será instanciado. Deve-se instanciar manualmente os campos ignorados
	 * 
	 * @param c
	 * @return
	 */
	public static <T> T initFull(Class<T> c)
	{
		T o = null;
		try
		{
			o = c.newInstance();
			if(o != null)
			{
				for(Field f : c.getDeclaredFields())
				{
					if(cannotInit(f))
						continue;

					if(AppEntity.class.isAssignableFrom(f.getType()))
					{
						f.setAccessible(true);
						f.set(o, initFull(f.getType()));
					}
				}
			}
		} catch(InstantiationException | IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return o;
	}

	public static boolean cannotInit(Field f)
	{
		return Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(Transient.class) || f.isAnnotationPresent(Ignore.class);
	}

	/**
	 * Inicializa os atributos null da entity.
	 * Cuidado com mapeamentos redundantes, provavelmente terá problema de StackOverflow.<br/>
	 * 
	 * Se usou este método para inicializar, utilize {@link ReflectionUtils#removeEmpty(Object)} antes de persistir.
	 * <br/><br/>
	 * 
	 * Para mapeamentos redundantes, coloque a anotação {@link Ignore} no campo que ocorre redundância.
	 * Ele não será instanciado. Deve-se instanciar manualmente os campos ignorados
	 * 
	 * @param c
	 * @param nullOnly
	 * @return
	 */
	public static <T> void initNull(Object o)
	{
		try
		{
			if(o != null)
			{
				Class c = o.getClass();
				for(Field f : c.getDeclaredFields())
				{
					if(cannotInit(f))
						continue;

					if(AppEntity.class.isAssignableFrom(f.getType()))
					{
						f.setAccessible(true);
						if(f.get(o) == null)
						{
							f.set(o, initFull(f.getType()));
						}
					}
				}
			}
		} catch(IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Semelhante a {@link ReflectionUtils#initNull(Object)} porém só inicializa atributos anotados com {@link Transient}
	 * @see {@link ReflectionUtils#initNull(Object)}
	 * @param o
	 */
	public static <T> void initTransient(Object o)
	{
		try
		{
			if(o != null)
			{
				Class c = o.getClass();
				for(Field f : c.getDeclaredFields())
				{
					if(!f.isAnnotationPresent(Transient.class))
						continue;

					if(AppEntity.class.isAssignableFrom(f.getType()))
					{
						f.setAccessible(true);
						if(f.get(o) == null)
						{
							f.set(o, initFull(f.getType()));
						}
					}
				}
			}
		} catch(IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Este método deixa nulo todos os atributos que são objetos vazios (deu new ...() e não preencheu).
	 * Usado para limpar entidades antes de persistir para não dar exceção de entidade não sincronizada.
	 * 
	 * @param o
	 * @return
	 */
	public static Object removeEmpty(Object o)
	{
		try
		{
			if(o != null)
			{
				Class c = o.getClass();
				for(Field f : c.getDeclaredFields())
				{
					if(cannotInit(f))
						continue;

					if(AppEntity.class.isAssignableFrom(f.getType()))
					{
						f.setAccessible(true);
						Object attr = removeEmpty(f.get(o));
						if(attr == null)
						{
							f.set(o, null);
						}
					}
				}
			}
			if(isFullEmpty(o))
				return null;
		} catch(IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * Verifica se todos os atributos do objeto está vazio (isto é, chamou construtor vazio porém nao utilizou o objeto)
	 * 
	 * @param o
	 * @return
	 */
	public static boolean isFullEmpty(Object o)
	{
		boolean empty = true;
		try
		{
			if(o != null)
			{
				Class c = o.getClass();
				for(Field f : c.getDeclaredFields())
				{
					if(cannotInit(f))
						continue;

					f.setAccessible(true);
					if(AppEntity.class.isAssignableFrom(f.getType()))
					{
						empty &= isFullEmpty(f.get(o));
					}
					/*if(f.getType().isPrimitive())
					{
						empty &= Defaults.defaultValue(f.getType()).equals(f.get(o));
					} else
					{
						empty &= f.get(o) == null;
					}*/

					if(!empty)
						break;
				}
			}
		} catch(IllegalAccessException | IllegalArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return empty;
	}

	/**
	 * Usado quando deseja inicializar atributos Lazy. A entity deve estar sincronizada com a persistência.
	 * Há 03 opções de inicialização:
	 * <pre>
	 * ignoreOnly = null   - Inicializará todos os atributos lazy, anotados com {@link Ignore} ou não;
	 * ignoreOnly = true   - Inicializará somente atributos lazy anotados com {@link Ignore};
	 * ignoreOnly = false  - Inicializará somente atributos lazy não anotados com {@link Ignore};
	 * </pre>
	 * 
	 * @param lazyEntity
	 * @throws IllegalAccessException
	 */
	public static void initializeLazyEntity(Object lazyEntity, Integer depthLeft, Boolean ignoreOnly)
	{
		if(depthLeft == null)
			depthLeft = 0;
		if(lazyEntity == null || depthLeft < 0)
			return;
		for(Field field : lazyEntity.getClass().getDeclaredFields())
		{
			try
			{
				boolean cannotInit = cannotInit(field);
				boolean canCopy = (ignoreOnly == null) || (ignoreOnly && cannotInit) || (!ignoreOnly && !cannotInit);
				if(!canCopy)
					continue;

				if(field.isAnnotationPresent(OneToOne.class))
				{
					OneToOne annotation = field.getAnnotation(OneToOne.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						initializeLazyField(lazyEntity, field);
					}
					field.setAccessible(true);
					initializeLazyEntity(field.get(lazyEntity), depthLeft - 1, ignoreOnly);
				}
				if(field.isAnnotationPresent(ManyToOne.class))
				{
					ManyToOne annotation = field.getAnnotation(ManyToOne.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						initializeLazyField(lazyEntity, field);
					}
					field.setAccessible(true);
					initializeLazyEntity(field.get(lazyEntity), depthLeft - 1, ignoreOnly);
				}
				if(field.isAnnotationPresent(OneToMany.class))
				{
					OneToMany annotation = field.getAnnotation(OneToMany.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						initializeLazyField(lazyEntity, field);
					}
				}
				if(field.isAnnotationPresent(ManyToMany.class))
				{
					ManyToMany annotation = field.getAnnotation(ManyToMany.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						initializeLazyField(lazyEntity, field);
					}
				}
			} catch(Exception e)
			{
				e.printStackTrace();
				//				LOG.error("error initializing: " + lazyEntity.getClass().getSimpleName() + "#" + field.getName(), e);
			}
		}
	}

	/**
	 * Usado quanto deseja inicializar os atributos Lazy porém sem recarregar os atributos da entity. (Ex: Alguma propriedade da entity alterou, posteriormente pode acontecer um alterar). <br/>
	 * <ul>
	 * <li>LazyEntity não está sincronizado com a persistência;</li>
	 * <li>EagerEntity está sincronizado com a persistência;</li>
	 * </ul>
	 * Há 03 maneiras possíveis de fazer o copy:
	 * <pre>
	 * ignoreOnly = null   - Inicializará todos os atributos lazy, anotados com {@link Ignore} ou não;
	 * ignoreOnly = true   - Inicializará somente atributos lazy anotados com {@link Ignore};
	 * ignoreOnly = false  - Inicializará somente atributos lazy não anotados com {@link Ignore};
	 * </pre>
	 * 
	 * @param lazyEntity
	 * @param eagerEntity
	 * @param ignoreOnly
	 */
	public static void copyEagerEntity(Object lazyEntity, Object eagerEntity, Boolean ignoreOnly)
	{
		if(!hasLazyAttribute(lazyEntity))
			return;
		for(Field field : eagerEntity.getClass().getDeclaredFields())
		{
			try
			{
				boolean cannotInit = cannotInit(field);
				boolean canCopy = (ignoreOnly == null) || (ignoreOnly && cannotInit) || (!ignoreOnly && !cannotInit);
				if(!canCopy)
					continue;

				if(field.isAnnotationPresent(OneToOne.class))
				{
					OneToOne annotation = field.getAnnotation(OneToOne.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						copyEagerField(lazyEntity, eagerEntity, field);
					}
				}
				if(field.isAnnotationPresent(ManyToOne.class))
				{
					ManyToOne annotation = field.getAnnotation(ManyToOne.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						copyEagerField(lazyEntity, eagerEntity, field);
					}
				}
				if(field.isAnnotationPresent(OneToMany.class))
				{
					OneToMany annotation = field.getAnnotation(OneToMany.class);
					if(annotation.fetch() == FetchType.LAZY)
					{
						copyEagerField(lazyEntity, eagerEntity, field);
					}
				}
			} catch(Exception e)
			{
				e.printStackTrace();
				//				LOG.error("error initializing: " + lazyEntity.getClass().getSimpleName() + "#" + field.getName(), e);
			}
		}
	}

	private static void copyEagerField(Object lazyEntity, Object eagerEntity, Field field) throws IllegalAccessException
	{
		Object lazy = initializeLazyField(eagerEntity, field);
		field.set(lazyEntity, lazy);
	}

	private static Object initializeLazyField(Object lazyEntity, Field field) throws IllegalAccessException
	{
		field.setAccessible(true);
		Object lazy = field.get(lazyEntity);
		if(lazy instanceof HibernateProxy || lazy instanceof PersistentCollection)
			Hibernate.initialize(lazy);
		return lazy;
	}

	/**
	 * Verifica se a Entity possui algum tipo de atributo Lazy.
	 * 
	 * @param lazyEntity
	 * @return
	 */
	public static boolean hasLazyAttribute(Object lazyEntity)
	{
		if(lazyEntity == null)
			return false;
		for(Field field : lazyEntity.getClass().getDeclaredFields())
		{
			if(cannotInit(field))
				continue;

			if(field.isAnnotationPresent(OneToOne.class))
			{
				OneToOne annotation = field.getAnnotation(OneToOne.class);
				if(annotation.fetch() == FetchType.LAZY)
					return true;
			}
			if(field.isAnnotationPresent(ManyToOne.class))
			{
				ManyToOne annotation = field.getAnnotation(ManyToOne.class);
				if(annotation.fetch() == FetchType.LAZY)
					return true;
			}
			if(field.isAnnotationPresent(OneToMany.class))
			{
				OneToMany annotation = field.getAnnotation(OneToMany.class);
				if(annotation.fetch() == FetchType.LAZY)
					return true;
			}
		}
		return false;
	}

	

	public static String toNativeField(String query, Class entity, String alias)
	{
		String result = query;
		String aliasStr = StringUtils.isBlank(alias) ? "" : alias + ".";

		List<Field> fields = Arrays.asList(entity.getDeclaredFields());
		Collections.sort(fields, new Comparator<Field>()
		{
			@Override
			public int compare(Field o1, Field o2)
			{
				return Integer.valueOf(o2.getName().length()).compareTo(Integer.valueOf(o1.getName().length()));
			}
		});

		for(Field field : fields)
		{
			String nativeName = nativeField(field);
			result = result.replaceAll("(?i)" + nativeName, aliasStr + field.getName());
		}

		return result;
	}

	private static String nativeField(Field field)
	{
		String nativeName = field.getName();
		Column annotation = field.getAnnotation(Column.class);
		if(annotation != null && StringUtils.isNotBlank(annotation.name()))
			nativeName = annotation.name();
		return nativeName;
	}

	public static boolean containsClass(Collection c, Class x)
	{
		for(Object o : c)
			if(o.getClass() == x)
				return true;
		return false;
	}

	public static <T> T coalesce(T... objects)
	{
		for(T o : objects)
			if(o != null)
				return o;
		return null;
	}

}
