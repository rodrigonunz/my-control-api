package com.mycontrol.api.controllers;


import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycontrol.api.model.Lancamento;
import com.mycontrol.api.repositories.LancamentoRepository;

@RestController
@RequestMapping("/teste")
@CrossOrigin(origins = "*")
public class TesteController {
	
	@Autowired
	LancamentoRepository repo;
	
	
	@GetMapping("/cria")
	public String cria(){
		Lancamento lancamento = new Lancamento();
		lancamento.setDescricao("testeeeee");
		lancamento.setTipo("Despesa");
		repo.save(lancamento);
		
		return "ok";
	}
	
	@GetMapping("/altera")
	public String altera(){
		Lancamento lanc = repo.findById(1);
		lanc.setDescricao("Nova descricao".concat(new Date().toString()));
		
		repo.save(lanc);
		
		
		return "ok";
	}

}
