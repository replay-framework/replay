package models;

public class Verdict {
  public final boolean canBeFree;
  public final String explanation;

  public Verdict(boolean canBeFree, String explanation) {
    this.canBeFree = canBeFree;
    this.explanation = explanation;
  }
}
