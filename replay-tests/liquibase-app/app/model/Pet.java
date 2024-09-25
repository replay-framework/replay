package model;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import play.data.binding.NoBinding;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.MinSize;
import play.data.validation.Required;

@Entity
public class Pet extends IdModel {
  @Enumerated(STRING)
  @Required
  public Kind kind;

  @Required
  @MinSize(3)
  public String name;

  @Required
  @Min(0)
  @Max(100)
  public int age;

  @Column(name = "created_at", nullable = false)
  @NoBinding
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  @NoBinding
  public LocalDateTime updatedAt;

  public Pet(
      long id, Kind kind, String name, int age, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.kind = kind;
    this.name = name;
    this.age = age;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Pet() {
    this(0, null, null, -1, null, null);
  }
}
