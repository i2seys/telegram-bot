package ru.savenkov.SpringBot.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.savenkov.SpringBot.model.UserCondition;

import java.util.List;

@Repository
public interface UserConditionRepository extends CrudRepository<UserCondition, Long> {
}
