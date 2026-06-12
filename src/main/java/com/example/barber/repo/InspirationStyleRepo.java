package com.example.barber.repo;

import com.example.barber.model.InspirationStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
  Repozytorium JPA do operacji na encji {@link InspirationStyle} (style inspiracji).

  <p>Dziedziczy z {@link JpaRepository} i automatycznie dostarcza pełny
  zestaw operacji CRUD ({@code save}, {@code findById}, {@code findAll},
  {@code deleteById} itp.) bez konieczności pisania implementacji.</p>

  <p>Aktualnie nie definiuje własnych metod zapytań – wszystkie potrzebne operacje
  są pokryte przez metody odziedziczone z {@code JpaRepository}.</p>
 */
@Repository
public interface InspirationStyleRepo extends JpaRepository<InspirationStyle, Long> {
}
