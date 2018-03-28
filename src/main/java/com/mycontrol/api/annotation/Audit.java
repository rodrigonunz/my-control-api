package com.mycontrol.api.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
public @interface Audit {
	/**
	 * Usado nas entidades
	 * 
	 * @return
	 */
	AuditMethod[] onClass() default { AuditMethod.SALVAR, AuditMethod.ALTERAR, AuditMethod.EXCLUIR };

	/**
	 * Usado em metodos de persistência de XxxService que herdam {@link CrudService}
	 * 
	 * @return
	 */
	AuditMethod onMethod() default AuditMethod.NONE;

	/**
	 * OBS: o nome destes enums serão registrados como ação em {@link LogDB#acao}, entao devem ter no maximo 8 caracteres
	 */
	public enum AuditMethod
	{
		NONE(""),

		/**
		 * Params:
		 * 	<ul>
		 * 		<li>[opcional] ScpiConstants.ENTITY.getName() -> Se enviar uma Entity como parâmetro, mesmo que seja vazia, salva a tabela associada a operação genérica</li>
		 * 	</ul>
		 */
		GENERICO("OPERACAO"),

		/**
		 * Params:
		 * 	<ul>
		 * 		<li>ScpiConstants.ENTITY.getName()</li>
		 * 	</ul>
		 */
		SALVAR("SALVAR"),
		/**
		 * Params:
		 * 	<ul>
		 * 		<li>ScpiConstants.ENTITY.getName()</li>
		 * 	</ul>
		 */
		BUSCAR("BUSCAR"),
		/**
		 * Params:
		 * 	<ul>
		 * 		<li>ScpiConstants.ENTITY.getName()</li>
		 * 	</ul>
		 */
		ALTERAR("ALTERAR"),
		/**
		 * Params:
		 * 	<ul>
		 * 		<li>ScpiConstants.ENTITY.getName()</li>
		 * 	</ul>
		 */
		EXCLUIR("EXCLUIR"),
		/**
		 * Params:
		 * 	<ul>
		 * 	</ul>
		 */
		LISTAR("LISTAR");

		private String acao;

		private AuditMethod(String acao)
		{
			this.acao = acao;
		}

		public String getAcao()
		{
			return acao;
		}

	}
}
