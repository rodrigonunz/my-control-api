package com.mycontrol.api.services.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mycontrol.api.model.Usuario;
import com.mycontrol.api.repositories.UsuarioRepository;
import com.mycontrol.api.services.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {
	
	@Autowired
	private UsuarioRepository repo;

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return Optional.ofNullable(repo.findByEmail(email));
	}

}
