package com.mycontrol.api.enums;

public enum Constantes {
	LOG_AUDIT_METHOD("LOG_AUDIT_METHOD"), LOG_METHOD_NAME("LOG_METHOD_NAME"), LOG_ENTITY("LOG_ENTITY"), ENTITY_ORIGINAL(
			"ENTITY_ORIGINAL");

	private String name;

	private Constantes(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
