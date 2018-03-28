package com.mycontrol.api.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.mycontrol.api.annotation.Audit;
import com.mycontrol.api.annotation.Audit.AuditMethod;
import com.mycontrol.api.enums.Constantes;

public class AuditUtils {


	/**
	 * Este método não pode lançar exceção e interromper a execução.
	 * 
	 * @param ctx
	 * @param context
	 */
	public static void audit(Context context) {
		try {
			persistLog(context);

		} catch (Exception e) {
			// LOG.error(e);
		}
	}



	/**
	 * Registra efetivamente o log no banco de dados.
	 * 
	 * @param context
	 * @throws FiorilliException
	 */
	private static void persistLog(Context context) throws Exception {
		String auditTrail = getAuditTrail(context);
		if (auditTrail == null)
			return;

		Object o = context.get(Constantes.LOG_ENTITY.getName());
		AuditMethod m = (AuditMethod) context.get(Constantes.LOG_AUDIT_METHOD.getName());
		//Usuario usuario = (Usuario) context.get(Constantes.USUARIO_LOGADO.getName());

		String tabela = o != null ? ReflectionUtils.getTableName(o.getClass()) : null;

		Date now = new Date();

		//gera o log
	}

	/**
	 * Gera a trilha de auditoria apresentada na descrição do log. A trilha é
	 * gerada de acordo com o tipo de método (salvar, alterar, excluir, ...)
	 * 
	 * @param context
	 * @return
	 */
	private static String getAuditTrail(Context context) {
		try {
			AuditMethod m = (AuditMethod) context.get(Constantes.LOG_AUDIT_METHOD.getName());
			String methodName = (String) context.get(Constantes.LOG_METHOD_NAME.getName());
			Object o = context.get(Constantes.LOG_ENTITY.getName());
			Object clone = context.get(Constantes.ENTITY_ORIGINAL.getName());

			switch (m) {
			case GENERICO:
				return info(o, methodName);
			case SALVAR:
				return create(o);
			case EXCLUIR:
				return delete(o);
			case ALTERAR:
				return update(o, clone);
			case LISTAR:
				return list(o.getClass());
			case BUSCAR:
				return read(o);
			case NONE:
			default:
				break;
			}
			// Não deveria chegar aqui
			return "operação não identificada";
		} catch (Exception e) {
			//LOG.error(e);
			return e.getMessage();
		}
	}

	/**
	 * Gera a trilha de informações da entidade. Lista todos os atributos.
	 * 
	 * @param o
	 * @param methodName
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static String info(Object o, String methodName)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (o != null) {
			String id = ReflectionUtils.getIdAsText(o);
			String tabela = ReflectionUtils.getTableName(o.getClass());
			return "Operação em " + tabela + " com ID = " + id + "\nDetalhes:\n" + methodName;
		}
		return methodName;
	}

	/**
	 * Gera a trilha de update. Lista a comparação de todos os atributos que
	 * alteraram o valor.
	 * 
	 * @param o
	 * @param old
	 * @return
	 * @throws IllegalAccessException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws NoSuchFieldException
	 */
	private static String update(Object o, Object old)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
		String id = ReflectionUtils.getIdAsText(o);
		String tabela = ReflectionUtils.getTableName(o.getClass());
		String trail = ReflectionUtils.getDifference(o, old);
		if (StringUtils.isBlank(trail))
			return null;
		return "Alterou em " + tabela + " com ID = " + id + "\n" + "Diferença:\n" + trail;
	}

	/**
	 * Gera trilha de listagem.
	 * 
	 * @param c
	 * @return
	 */
	private static String list(Class c) {
		String tabela = ReflectionUtils.getTableName(c);
		return "Listou em " + tabela;
	}

	/**
	 * Gera trilha de remoção. Exibe o estado dos atributos do objeto removido
	 * naquele instante.
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static String delete(Object o)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String id = ReflectionUtils.getIdAsText(o);
		String tabela = ReflectionUtils.getTableName(o.getClass());
		String trail = ReflectionUtils.attributes(o, true);
		return "Excluiu em " + tabela + " com ID = " + id + "\nDetalhes:\n" + trail;
	}

	/**
	 * Gera a trilha de inserção. Exibe o estado dos atributos do objeto naquele
	 * instante.
	 * 
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static String create(Object o)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String id = ReflectionUtils.getIdAsText(o);
		String tabela = ReflectionUtils.getTableName(o.getClass());
		String trail = ReflectionUtils.attributes(o, true);
		return "Criou em " + tabela + " com ID = " + id + "\nDetalhes:\n" + trail;
	}

	/**
	 * Gera a trilha de leitura. Informa qual id foi pesquisado.
	 * 
	 * @param o
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static String read(Object o)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		String id = ReflectionUtils.getIdAsText(o);
		String tabela = ReflectionUtils.getTableName(o.getClass());
		return "Buscou em " + tabela + " com ID = " + id;
	}

	/**
	 * Escaneia o método executado no XxxService procurando
	 * {@link Audit#onMethod()}
	 * 
	 * @param service
	 * @param methodName
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static AuditMethod getAuditMethod(Class service, String methodName)
			throws NoSuchMethodException, SecurityException {
		Audit annotation = (Audit) ReflectionUtils.scanMethodForAnnotation(service, methodName, Audit.class);
		if (annotation != null && annotation.onMethod() != null && annotation.onMethod() != AuditMethod.NONE)
			return annotation.onMethod();
		return null;
	}

	/**
	 * Verifica se o método executado deve ser auditado.
	 * 
	 * @param entity
	 * @param auditMethod
	 * @return
	 */
	private static boolean checkEntityMethod(Object entity, AuditMethod auditMethod) {
		if (AuditMethod.GENERICO == auditMethod)
			return true;
		if (entity == null || auditMethod == null)
			return false;
		Audit annotationOnClass = entity.getClass().getAnnotation(Audit.class);
		if (annotationOnClass != null) {
			AuditMethod[] onClass = annotationOnClass.onClass();
			for (AuditMethod method : onClass) {
				if (method == auditMethod) {
					return true;
				}
			}
		}
		return false;
	}

}
