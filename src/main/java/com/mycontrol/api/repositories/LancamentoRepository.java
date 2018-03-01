package com.mycontrol.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mycontrol.api.model.Lancamento;

@Transactional(readOnly = true)
public interface LancamentoRepository extends JpaRepository<Lancamento, Integer> {
	
	Lancamento findById(Integer id);

}
