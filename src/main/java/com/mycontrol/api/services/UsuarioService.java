package com.mycontrol.api.services;

import java.util.Optional;

import com.mycontrol.api.model.Usuario;

public interface UsuarioService {
	
	Optional<Usuario> buscarPorEmail(String email);
	

}
