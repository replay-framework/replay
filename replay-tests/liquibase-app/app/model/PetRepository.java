package model;

import play.db.jpa.JPA;

import javax.inject.Singleton;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;

@Singleton
public class PetRepository {
  public List<Pet> loadAllPets() {
    return JPA.em().createQuery("select p from Pet p where p.createdAt > ?1 order by p.id", Pet.class)
      .setParameter(1, LocalDateTime.now().minusYears(5))
      .getResultList();
  }

  public void register(Pet pet) {
    pet.createdAt = LocalDateTime.now();
    pet.updatedAt = LocalDateTime.now();
    pet.save();
  }

  public int deleteAllPets() {
    Query query = JPA.em().createQuery("delete from Pet p");
    return query.executeUpdate();
  }
}
