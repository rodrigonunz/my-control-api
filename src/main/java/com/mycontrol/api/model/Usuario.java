package com.mycontrol.api.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.mycontrol.api.enums.PerfilEnum;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9184610134309749100L;
	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(name = "email", nullable = false, length = 300)
	private String email;
	
	@Column(name = "senha", nullable = false, length = 300)
	private String senha;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "perfil", nullable = false)
	private PerfilEnum perfil;
	
	
	@Column(name = "nome_sobrenome", nullable = false, length = 300)
	private String nomeSobrenome;


	public String getNomeSobrenome() {
		return nomeSobrenome;
	}


	public void setNomeSobrenome(String nomeSobrenome) {
		this.nomeSobrenome = nomeSobrenome;
	}


	public PerfilEnum getPerfil() {
		return perfil;
	}


	public void setPerfil(PerfilEnum perfil) {
		this.perfil = perfil;
	}


	public String getSenha() {
		return senha;
	}


	public void setSenha(String senha) {
		this.senha = senha;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	@Override
	public String toString() {
		return "Usuario [id=" + id + ", email=" + email + ", senha=" + senha + ", perfil=" + perfil + ", nomeSobrenome="
				+ nomeSobrenome + "]";
	}
	
}
